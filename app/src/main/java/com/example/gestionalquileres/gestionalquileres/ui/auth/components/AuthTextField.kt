package com.example.gestionalquileres.ui.auth.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.gestionalquileres.ui.common.accionDeEnter
import com.example.gestionalquileres.ui.common.teclasDeFormulario

/**
 * Campo de texto con el estilo de las pantallas de autenticación.
 *
 * - Siempre es de una sola línea: Tab y Enter nunca se escriben como carácter.
 * - Tab / Shift+Tab mueven el foco al campo siguiente / anterior.
 * - Enter (o el botón de acción del teclado en pantalla):
 *     · con [imeAction] = Next  → pasa al siguiente campo
 *     · con [imeAction] = Done  → oculta el teclado y ejecuta [onDone]
 */
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onDone: () -> Unit = {}
) {
    val enter = accionDeEnter(imeAction, onDone)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { enter() },
            onDone = { enter() }
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .teclasDeFormulario(enter),
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            unfocusedBorderColor = Color.White,
            focusedBorderColor = Color.White,
            unfocusedLabelColor = Color.Gray,
            focusedLabelColor = Color.White
        )
    )
}