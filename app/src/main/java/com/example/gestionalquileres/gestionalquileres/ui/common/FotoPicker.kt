package com.example.gestionalquileres.ui.common

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Muestra una imagen Base64 (con caché de Coil) o el [placeholder] si no hay foto. */
@Composable
fun ImagenBase64(
    base64: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: @Composable () -> Unit = {}
) {
    if (base64.isNullOrBlank()) {
        placeholder()
    } else {
        val modelo = remember(base64) { FotoBase64(base64) }
        AsyncImage(
            model = modelo,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

/**
 * Vista previa + botones para elegir, cambiar o quitar una fotografía.
 * [onFotoChange] recibe (miniatura, foto grande); ambos null = se quitó la foto.
 */
@Composable
fun FotoPicker(
    fotografiaBase64: String?,
    onFotoChange: (miniatura: String?, completa: String?) -> Unit,
    modifier: Modifier = Modifier,
    enfocarSolicitud: Int = 0
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                withContext(Dispatchers.IO) { uriAFotoProcesada(context, uri) }
                    ?.let { onFotoChange(it.miniatura, it.completa) }
            }
        }
    }

    val abrirGaleria: () -> Unit = {
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val focoBoton = remember { FocusRequester() }

    LaunchedEffect(enfocarSolicitud) {
        if (enfocarSolicitud > 0) focoBoton.requestFocus()
    }

    val hayFoto = !fotografiaBase64.isNullOrBlank()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            ImagenBase64(
                base64 = fotografiaBase64,
                modifier = Modifier.size(90.dp),
                contentDescription = "Fotografía"
            ) {
                Text(
                    text = "Sin foto",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = abrirGaleria,
                modifier = Modifier.focusRequester(focoBoton)
            ) {
                Text(if (hayFoto) "Cambiar foto" else "Elegir foto")
            }

            if (hayFoto) {
                TextButton(onClick = { onFotoChange(null, null) }) {
                    Text("Quitar foto", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}