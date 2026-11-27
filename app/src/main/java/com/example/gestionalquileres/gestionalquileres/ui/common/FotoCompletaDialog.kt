package com.example.gestionalquileres.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gestionalquileres.domain.repository.TipoFoto

/** Carga la foto grande solo cuando el usuario la pide. Mientras tanto muestra la miniatura. */
@Composable
fun FotoCompletaDialog(
    tipo: TipoFoto,
    idPropietario: String,
    miniatura: String?,
    onDismiss: () -> Unit,
    viewModel: FotoViewModel = hiltViewModel()
) {
    // null = cargando; "" = no hay versión grande (registros antiguos)
    val completa by produceState<String?>(null, tipo, idPropietario) {
        value = viewModel.obtener(tipo, idPropietario).orEmpty()
    }
    val mostrar = completa?.takeIf { it.isNotEmpty() } ?: miniatura

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ImagenBase64(
                    base64 = mostrar,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    contentDescription = "Fotografía",
                    contentScale = ContentScale.Fit
                )
                if (completa == null) CircularProgressIndicator()
            }
        }
    )
}