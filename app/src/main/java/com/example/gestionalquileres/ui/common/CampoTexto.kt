package com.example.gestionalquileres.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Campo de texto estándar de los formularios.
 * Enter pasa al siguiente campo ([ImeAction.Next]). En el último campo usa
 * `imeAction = ImeAction.Done` y `onDone` para ejecutar el formulario.
 */
@Composable
fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    teclado: KeyboardType = KeyboardType.Text,
    soloDigitos: Boolean = false,
    enabled: Boolean = true,
    esError: Boolean = false,
    ayuda: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    onDone: () -> Unit = {}
) {
    val enter = accionDeEnter(imeAction, onDone)

    OutlinedTextField(
        value = valor,
        onValueChange = { onCambio(if (soloDigitos) it.filter(Char::isDigit) else it) },
        label = { Text(etiqueta) },
        enabled = enabled,
        isError = esError,
        supportingText = ayuda?.let { texto -> { Text(texto) } },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = teclado, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { enter() },
            onDone = { enter() }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .teclasDeFormulario(enter)
    )
}