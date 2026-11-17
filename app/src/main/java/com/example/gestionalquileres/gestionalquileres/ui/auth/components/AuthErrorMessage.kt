package com.example.gestionalquileres.ui.auth.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Muestra el error del [AuthUiState] y lo limpia solo a los 3 segundos. */
@Composable
fun AuthErrorMessage(
    errorMessage: String?,
    onClearError: () -> Unit
) {
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            delay(3000)
            onClearError()
        }
    }

    if (errorMessage != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = errorMessage,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
