package com.example.gestionalquileres.ui.auth

import android.security.keystore.KeyPermanentlyInvalidatedException
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionalquileres.data.auth.AuthRepository
import com.example.gestionalquileres.data.auth.BiometricCredentialStore
import com.example.gestionalquileres.data.auth.CredencialesGuardadas
import com.example.gestionalquileres.domain.model.UserRole
import com.example.gestionalquileres.domain.model.onFailure
import com.example.gestionalquileres.domain.model.onSuccess
import com.example.gestionalquileres.notificaciones.ProgramadorAlertas
import com.example.gestionalquileres.notificaciones.SesionAlertas
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.crypto.Cipher
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val credenciales: BiometricCredentialStore,
    private val sesionAlertas: SesionAlertas,
    private val programadorAlertas: ProgramadorAlertas
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(isLoading = true))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // --- Huella dactilar (flujos aparte: uiState se reinicia en cada operación) ---
    private val _huellaActivada = MutableStateFlow(credenciales.hayCredenciales())
    val huellaActivada: StateFlow<Boolean> = _huellaActivada.asStateFlow()

    private val _ofrecerHuella = MutableStateFlow(false)
    val ofrecerHuella: StateFlow<Boolean> = _ofrecerHuella.asStateFlow()

    /** Credenciales del último login manual, solo en memoria hasta que el usuario decida. */
    private var credencialesPendientes: CredencialesGuardadas? = null

    init {
        loadActiveSession()
        guardarRolParaAlertas()
    }

    fun register(
        email: String,
        password: String,
        confirmPassword: String,
        role: UserRole = UserRole.SECRETARIO // opcional si quieres conservar la firma externa
    ) {
        when {
            email.trim().isEmpty() -> showError("Ingresa tu correo.")
            password.length < 6 -> showError("La contraseña debe tener al menos 6 caracteres.")
            password != confirmPassword -> showError("Las contraseñas no coinciden.")
            else -> {
                viewModelScope.launch {
                    _uiState.value = AuthUiState(isLoading = true)

                    repository.register(email.trim(), password)
                        .onSuccess { user ->
                            _uiState.value = AuthUiState(currentUser = user)
                        }
                        .onFailure { error ->
                            _uiState.value = AuthUiState(
                                errorMessage = error.message ?: "No se pudo registrar el usuario."
                            )
                        }
                }
            }
        }
    }

    fun login(email: String, password: String) {
        val emailLimpio = email.trim()

        when {
            emailLimpio.isEmpty() -> {
                showError("Ingresa tu correo.")
            }

            !Patterns.EMAIL_ADDRESS.matcher(emailLimpio).matches() -> {
                showError("Ingresa un correo electrónico válido.")
            }

            password.isEmpty() -> {
                showError("Ingresa tu contraseña.")
            }

            else -> iniciarSesion(emailLimpio, password, desdeHuella = false)
        }
    }

    private fun iniciarSesion(email: String, password: String, desdeHuella: Boolean) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)

            repository.login(email, password)
                .onSuccess { user ->
                    _uiState.value = AuthUiState(currentUser = user)

                    // Tras un login manual, ofrecer activar la huella si se puede y aún no está activa
                    if (!desdeHuella && credenciales.sensorDisponible() && !_huellaActivada.value) {
                        credencialesPendientes = CredencialesGuardadas(email, password)
                        _ofrecerHuella.value = true
                    }
                }
                .onFailure { error ->
                    val credencialInvalida = error is FirebaseAuthInvalidCredentialsException ||
                            error is FirebaseAuthInvalidUserException

                    if (desdeHuella && credencialInvalida) {
                        // La contraseña cambió o la cuenta ya no existe: lo guardado ya no sirve
                        olvidarHuella()
                        _uiState.value = AuthUiState(
                            errorMessage = "Tus datos guardados ya no son válidos. " +
                                    "Inicia sesión con tu correo y contraseña."
                        )
                    } else {
                        _uiState.value = AuthUiState(
                            errorMessage = error.message
                                ?: "Credenciales inválidas. Verifica tu correo y contraseña."
                        )
                    }
                }
        }
    }

    fun signInWithGoogle(
        idToken: String,
        role: UserRole = UserRole.SECRETARIO // opcional si quieres conservar la firma externa
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)

            repository.signInWithGoogle(idToken)
                .onSuccess { user ->
                    _uiState.value = AuthUiState(currentUser = user)
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState(
                        errorMessage = error.message
                            ?: "No se pudo iniciar sesión con Google."
                    )
                }
        }
    }

    fun logout() {
        repository.logout()
        _uiState.value = AuthUiState()
    }

    private fun loadActiveSession() {
        viewModelScope.launch {
            if (repository.getCurrentUserId() == null) {
                _uiState.value = AuthUiState()
                return@launch
            }

            repository.getCurrentUser()
                .onSuccess { user ->
                    _uiState.value = AuthUiState(currentUser = user)
                }
                .onFailure {
                    repository.logout()
                    _uiState.value = AuthUiState()
                }
        }
    }

    // ------------------------------------------------------------------
    // Huella dactilar
    // ------------------------------------------------------------------

    fun huellaDisponible(): Boolean = credenciales.sensorDisponible()

    /** Cipher para el diálogo de ACTIVAR. Null si no se pudo preparar. */
    fun cipherParaActivarHuella(): Cipher? =
        runCatching { credenciales.cipherParaCifrar() }.getOrNull()

    /** Cipher para el diálogo de LOGIN. Si la clave quedó invalidada, limpia y avisa. */
    fun cipherParaLoginConHuella(): Cipher? = try {
        credenciales.cipherParaDescifrar()
    } catch (e: KeyPermanentlyInvalidatedException) {
        olvidarHuella()
        showError("Cambiaron las huellas del teléfono. Inicia sesión con tu contraseña y vuelve a activar la huella.")
        null
    } catch (e: Exception) {
        olvidarHuella()
        showError("No se pudo usar la huella. Inicia sesión con tu contraseña.")
        null
    }

    /** Devuelve true si se guardaron las credenciales. [cipher] ya viene autenticado. */
    fun activarHuella(cipher: Cipher): Boolean {
        val pendientes = credencialesPendientes ?: return false
        return try {
            credenciales.guardar(cipher, pendientes.correo, pendientes.contrasena)
            _huellaActivada.value = true
            descartarOfertaHuella()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun descartarOfertaHuella() {
        credencialesPendientes = null     // no dejar la contraseña en memoria
        _ofrecerHuella.value = false
    }

    fun loginConHuella(cipher: Cipher) {
        val guardadas = try {
            credenciales.leer(cipher)
        } catch (e: Exception) {
            olvidarHuella()
            showError("No se pudo usar la huella. Inicia sesión con tu contraseña.")
            return
        }
        iniciarSesion(guardadas.correo, guardadas.contrasena, desdeHuella = true)
    }

    fun olvidarHuella() {
        credenciales.borrar()
        _huellaActivada.value = false
    }

    /**
     * Enciende el sistema de alertas cuando hay un usuario con sesión y lo apaga cuando esa sesión se cierra.
     * Funciona mirando el estado, así que cubre cualquier forma de entrar que termine dejando un usuario ahí
     * (correo, Google, registro, sesión restaurada o huella, según cómo la hayas hecho).
     */
    private fun guardarRolParaAlertas() {
        viewModelScope.launch {
            var huboSesion = false
            uiState
                .map { it.currentUser }
                .distinctUntilChanged()
                .collect { user ->
                    if (user != null) {
                        val uid = repository.getCurrentUserId() ?: return@collect
                        val rol = if (user.role == UserRole.ADMIN.name) UserRole.ADMIN else UserRole.SECRETARIO
                        huboSesion = true
                        sesionAlertas.guardar(uid, rol)
                        programadorAlertas.activar()
                    } else if (huboSesion) {
                        huboSesion = false
                        programadorAlertas.desactivar()
                    }
                }
        }
    }

    fun showError(message: String) {
        _uiState.value = AuthUiState(errorMessage = message)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}