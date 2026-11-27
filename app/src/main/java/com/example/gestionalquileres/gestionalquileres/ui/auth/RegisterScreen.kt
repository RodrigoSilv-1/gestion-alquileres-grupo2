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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gestionalquileres.ui.auth.components.AuthBackground
import com.example.gestionalquileres.ui.auth.components.AuthErrorMessage
import com.example.gestionalquileres.ui.auth.components.AuthTextField
import com.example.gestionalquileres.ui.auth.components.GoogleSignInButton

@Composable
fun RegisterScreen(
    uiState: AuthUiState,
    onRegister: (String, String, String) -> Unit,
    onGoToLogin: () -> Unit,
    onClearError: () -> Unit,
    onGoogleSignIn: (String) -> Unit,
    onGoogleError: (String) -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    AuthBackground {
        Spacer(modifier = Modifier.height(50.dp))

        Text(
            text = "Crear cuenta",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        AuthTextField(
            value = email,
            onValueChange = { email = it },
            label = "Correo electrónico",
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        )

        Spacer(modifier = Modifier.height(12.dp))

        AuthTextField(
            value = password,
            onValueChange = { password = it },
            label = "Contraseña mínimo 6 caracteres",
            isPassword = true,
            imeAction = ImeAction.Next
        )

        Spacer(modifier = Modifier.height(12.dp))

        AuthTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Confirmar contraseña",
            isPassword = true,
            imeAction = ImeAction.Done,                // Enter en el último campo → registra
            onDone = { onRegister(email, password, confirmPassword) }
        )

        AuthErrorMessage(
            errorMessage = uiState.errorMessage,
            onClearError = onClearError
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                onRegister(email, password, confirmPassword)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1565C0),
                contentColor = Color.White
            )
        ) {
            Text("Registrarme", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(modifier = Modifier.height(12.dp))

        GoogleSignInButton(
            onGoogleToken = onGoogleSignIn,
            onGoogleError = onGoogleError
        )

        TextButton(onClick = onGoToLogin) {
            Text(
                text = "Ya tengo una cuenta",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
