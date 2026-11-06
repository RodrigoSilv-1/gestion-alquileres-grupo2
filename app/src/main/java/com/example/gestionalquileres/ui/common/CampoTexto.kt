package com.example.gestionalquileres.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** Campo de texto estándar de los formularios. */
@Composable
fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    teclado: KeyboardType = KeyboardType.Text,
    soloDigitos: Boolean = false,
    enabled: Boolean = true,
    esError: Boolean = false,
    ayuda: String? = null
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { onCambio(if (soloDigitos) it.filter(Char::isDigit) else it) },
        label = { Text(etiqueta) },
        enabled = enabled,
        isError = esError,
        supportingText = ayuda?.let { texto -> { Text(texto) } },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        modifier = Modifier.fillMaxWidth()
    )
}