package com.example.gestionalquileres.ui.common

import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
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

/** Muestra una imagen Base64 (decodificada fuera del hilo principal) o el [placeholder]. */
@Composable
fun ImagenBase64(
    base64: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: @Composable () -> Unit = {}
) {
    val bytes by produceState<ByteArray?>(null, base64) {
        value = if (base64.isNullOrBlank()) null
        else withContext(Dispatchers.Default) {
            runCatching { Base64.decode(base64, Base64.DEFAULT) }.getOrNull()
        }
    }

    val datos = bytes
    if (datos != null) {
        AsyncImage(
            model = datos,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        placeholder()
    }
}

/** Vista previa + botones para elegir, cambiar o quitar una fotografía. */
@Composable
fun FotoPicker(
    fotografiaBase64: String?,
    onFotoChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                withContext(Dispatchers.IO) { uriABase64(context, uri) }?.let(onFotoChange)
            }
        }
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
                onClick = {
                    launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            ) {
                Text(if (hayFoto) "Cambiar foto" else "Elegir foto")
            }

            if (hayFoto) {
                TextButton(onClick = { onFotoChange(null) }) {
                    Text("Quitar foto", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}