package com.example.gestionalquileres.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.gestionalquileres.ui.auth.AuthViewModel
import com.example.gestionalquileres.ui.auth.LoginScreen
import com.example.gestionalquileres.ui.auth.RegisterScreen
import com.example.gestionalquileres.ui.common.LoadingScreen
import com.example.gestionalquileres.ui.contrato.ContratoViewModel
import com.example.gestionalquileres.ui.home.HomeScreen
import com.example.gestionalquileres.ui.inmuebles.InmuebleViewModel
import com.example.gestionalquileres.ui.inquilinos.InquilinoViewModel
import com.example.gestionalquileres.ui.recibos.ReciboViewModel

/**
 * Raíz de la app: decide qué pantalla mostrar según el estado de la sesión
 * (cargando → login/registro → home según rol).
 */
@Composable
fun AppRoot(
    authViewModel: AuthViewModel,
    inquilinoViewModel: InquilinoViewModel,
    inmuebleViewModel: InmuebleViewModel,
    contratoViewModel: ContratoViewModel,
    reciboViewModel: ReciboViewModel
) {
    val uiState by authViewModel.uiState.collectAsState()
    var showRegister by rememberSaveable { mutableStateOf(false) }
    val currentUser = uiState.currentUser

    when {
        uiState.isLoading -> LoadingScreen()

        currentUser != null -> HomeScreen(
            user = currentUser,
            onLogout = authViewModel::logout,
            inquilinoViewModel = inquilinoViewModel,
            inmuebleViewModel = inmuebleViewModel,
            contratoViewModel = contratoViewModel,
            reciboViewModel = reciboViewModel
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
            onGoogleError = authViewModel::showError
        )
    }
}
