package com.example.gestionalquileres.ui.inquilinos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.gestionalquileres.domain.model.Inquilino
import com.example.gestionalquileres.domain.model.TipoFoto
import com.example.gestionalquileres.ui.common.FotoCompletaDialog
import com.example.gestionalquileres.ui.common.ImagenBase64

@Composable
fun InquilinoCard(
    inquilino: Inquilino,
    onModificar: (Inquilino) -> Unit,
    onDarDeBaja: (Inquilino) -> Unit,
    onSeleccionarFoto: (Inquilino) -> Unit
) {
    var verFoto by remember { mutableStateOf(false) }

    if (verFoto) {
        FotoCompletaDialog(
            tipo = TipoFoto.INQUILINO,
            idPropietario = inquilino.idInquilino,
            miniatura = inquilino.fotografia,
            onDismiss = { verFoto = false }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ImagenBase64(
                base64 = inquilino.fotografia,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !inquilino.fotografia.isNullOrBlank()) {
                        onSeleccionarFoto(inquilino)
                        verFoto = true
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = inquilino.nombre.firstOrNull()?.uppercase() ?: "?",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = inquilino.nombre,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "DNI: ${inquilino.dni} • Edad: ${inquilino.edad}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (inquilino.telefono.isNotBlank()) "Tel: ${inquilino.telefono}" else "Tel: Sin registrar",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row {
                IconButton(onClick = { onModificar(inquilino) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Modificar", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { onDarDeBaja(inquilino) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Dar de baja", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}