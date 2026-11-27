package com.example.gestionalquileres.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gestionalquileres.ui.auth.AuthViewModel
import com.example.gestionalquileres.ui.auth.LoginScreen
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.gestionalquileres.ui.auth.HuellaDactilar
import com.example.gestionalquileres.ui.auth.findFragmentActivity
import com.example.gestionalquileres.ui.auth.RegisterScreen
import com.example.gestionalquileres.ui.common.LoadingScreen

/**
 * Raíz de la app: decide qué mostrar según el estado de la sesión
 * (cargando → login/registro → navegación principal).
 */
@Composable
fun AppRoot(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by authViewModel.uiState.collectAsState()
    var showRegister by rememberSaveable { mutableStateOf(false) }
    val currentUser = uiState.currentUser

    val context = LocalContext.current
    val activity = context.findFragmentActivity()
    val huellaActivada by authViewModel.huellaActivada.collectAsState()
    val ofrecerHuella by authViewModel.ofrecerHuella.collectAsState()
    val huellaDisponible = remember { authViewModel.huellaDisponible() }

    val loginConHuella: () -> Unit = {
        val cipher = authViewModel.cipherParaLoginConHuella()
        if (cipher != null && activity != null) {
            HuellaDactilar.autenticar(
                activity = activity,
                cipher = cipher,
                titulo = "Iniciar sesión",
                subtitulo = "Usa tu huella para entrar",
                onExito = authViewModel::loginConHuella,
                onError = authViewModel::showError
            )
        }
    }

    BackHandler(enabled = showRegister && currentUser == null && !uiState.isLoading) {
        showRegister = false
        authViewModel.clearError()
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null) showRegister = false
    }

    when {
        uiState.isLoading -> LoadingScreen()

        currentUser != null -> AppNavHost(
            user = currentUser,
            onLogout = authViewModel::logout
        )

        showRegister -> RegisterScreen(
            uiState = uiState,
            onRegister = authViewModel::register,
            onGoToLogin = { showRegister = false },
            onClearError = authViewModel::clearError,
            onGoogleSignIn = authViewModel::signInWithGoogle,
            onGoogleError = authViewModel::showError
        )

        else -> LoginScreen(
            uiState = uiState,
            onLogin = authViewModel::login,
            onGoToRegister = { showRegister = true },
            onClearError = authViewModel::clearError,
            onGoogleSignIn = authViewModel::signInWithGoogle,
            onGoogleError = authViewModel::showError,
            huellaVisible = huellaDisponible && huellaActivada,
            onLoginConHuella = loginConHuella,
            onOlvidarHuella = authViewModel::olvidarHuella
        )
    }

    if (ofrecerHuella && currentUser != null) {
        AlertDialog(
            onDismissRequest = authViewModel::descartarOfertaHuella,
            title = { Text("¿Activar huella dactilar?") },
            text = { Text("La próxima vez podrás iniciar sesión usando tu huella en este dispositivo.") },
            confirmButton = {
                TextButton(onClick = {
                    val cipher = authViewModel.cipherParaActivarHuella()
                    if (cipher == null || activity == null) {
                        Toast.makeText(context, "No se pudo activar la huella.", Toast.LENGTH_SHORT).show()
                        authViewModel.descartarOfertaHuella()
                    } else {
                        HuellaDactilar.autenticar(
                            activity = activity,
                            cipher = cipher,
                            titulo = "Activar huella",
                            subtitulo = "Confirma tu huella para guardarla",
                            onExito = { autenticado ->
                                val ok = authViewModel.activarHuella(autenticado)
                                Toast.makeText(
                                    context,
                                    if (ok) "Huella activada." else "No se pudo activar la huella.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onError = { mensaje ->
                                Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }) { Text("Activar") }
            },
            dismissButton = {
                TextButton(onClick = authViewModel::descartarOfertaHuella) { Text("Ahora no") }
            }
        )
    }
}