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
import com.example.gestionalquileres.gestionalquileres.ui.auth.AuthViewModel
import com.example.gestionalquileres.gestionalquileres.ui.auth.LoginScreen
import com.example.gestionalquileres.gestionalquileres.ui.auth.RegisterScreen
import com.example.gestionalquileres.gestionalquileres.ui.common.LoadingScreen

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
            onGoogleError = authViewModel::showError
        )
    }
}