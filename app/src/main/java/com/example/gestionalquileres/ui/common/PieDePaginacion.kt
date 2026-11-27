package com.example.gestionalquileres.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Último elemento de una lista paginada: botón "Cargar más" o indicador de carga. */
@Composable
fun PieDePaginacion(
    cargando: Boolean,
    onCargarMas: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (cargando) CircularProgressIndicator() else OutlinedButton(onClick = onCargarMas) { Text("Cargar más") }
    }
}