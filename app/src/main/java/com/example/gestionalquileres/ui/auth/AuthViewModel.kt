package com.example.gestionalquileres.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionalquileres.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.gestionalquileres.domain.model.onSuccess
import com.example.gestionalquileres.domain.model.onFailure
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.gestionalquileres.domain.model.UserRole

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(isLoading = true))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        loadActiveSession()
    }

    fun register(
        email: String,
        password: String,
        confirmPassword: String,
        role: UserRole
    ) {
        when {
            email.trim().isEmpty() -> showError("Ingresa tu correo.")
            password.length < 6 -> showError("La contraseña debe tener al menos 6 caracteres.")
            password != confirmPassword -> showError("Las contraseñas no coinciden.")
            else -> {
                viewModelScope.launch {
                    _uiState.value = AuthUiState(isLoading = true)

                    repository.register(email.trim(), password, role)
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
        when {
            email.trim().isEmpty() -> showError("Ingresa tu correo.")
            password.isEmpty() -> showError("Ingresa tu contraseña.")
            else -> {
                viewModelScope.launch {
                    _uiState.value = AuthUiState(isLoading = true)

                    repository.login(email.trim(), password)
                        .onSuccess { user ->
                            _uiState.value = AuthUiState(currentUser = user)
                        }
                        .onFailure { error ->
                            _uiState.value = AuthUiState(
                                errorMessage = error.message ?: "Correo o contraseña incorrectos."
                            )
                        }
                }
            }
        }
    }
    fun signInWithGoogle(
        idToken: String,
        role: UserRole
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)

            repository.signInWithGoogle(idToken, role)
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

    fun showError(message: String) {
        _uiState.value = AuthUiState(errorMessage = message)
    }
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}