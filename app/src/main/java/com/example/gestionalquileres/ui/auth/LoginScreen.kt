package com.example.gestionalquileres.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gestionalquileres.domain.model.UserRole
import com.example.gestionalquileres.ui.auth.components.AuthBackground
import com.example.gestionalquileres.ui.auth.components.AuthErrorMessage
import com.example.gestionalquileres.ui.auth.components.AuthTextField
import com.example.gestionalquileres.ui.auth.components.GoogleSignInButton

@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onLogin: (String, String) -> Unit,
    onGoToRegister: () -> Unit,
    onClearError: () -> Unit,
    onGoogleSignIn: (String, UserRole) -> Unit,
    onGoogleError: (String) -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    AuthBackground {
        Spacer(modifier = Modifier.height(50.dp))
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Gestión de Alquileres",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Iniciar sesión",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        AuthTextField(
            value = email,
            onValueChange = { email = it },
            label = "Correo electrónico"
        )

        Spacer(modifier = Modifier.height(12.dp))

        AuthTextField(
            value = password,
            onValueChange = { password = it },
            label = "Contraseña",
            isPassword = true
        )

        AuthErrorMessage(
            errorMessage = uiState.errorMessage,
            onClearError = onClearError
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { onLogin(email, password) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1565C0),
                contentColor = Color.White
            )
        ) {
            Text("Iniciar sesión", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(modifier = Modifier.height(12.dp))

        GoogleSignInButton(
            role = UserRole.ADMIN,
            onGoogleToken = onGoogleSignIn,
            onGoogleError = onGoogleError
        )

        TextButton(onClick = onGoToRegister) {
            Text(
                text = "¿No tienes cuenta? Regístrate",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
