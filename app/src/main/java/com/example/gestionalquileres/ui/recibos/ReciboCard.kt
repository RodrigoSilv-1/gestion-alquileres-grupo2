package com.example.gestionalquileres.ui.recibos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.example.gestionalquileres.ui.theme.ExitoVerde
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gestionalquileres.domain.comoMonto
import com.example.gestionalquileres.domain.model.EstadoPago
import com.example.gestionalquileres.domain.model.Recibo

@Composable
fun ReciboCard(
    recibo: Recibo,
    actualizando: Boolean,
    onCambiarEstado: () -> Unit,
    onVerPdf: () -> Unit,
    onCompartirPdf: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        val pagado = recibo.estado() == EstadoPago.PAGADO
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = recibo.numeroUnico, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = recibo.estado().valor,
                    color = if (pagado) ExitoVerde else MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (recibo.nombreInquilino.isNotBlank()) {
                Text(text = "Inquilino: ${recibo.nombreInquilino}", fontWeight = FontWeight.Bold)
            }
            if (recibo.nombreUnidad.isNotBlank()) {
                Text(text = "Unidad: ${recibo.nombreUnidad}")
            }

            Text(text = "Fecha Emisión: ${recibo.fechaEmision}", style = MaterialTheme.typography.bodyMedium)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Usamos la función inteligente del modelo para mostrar solo los conceptos válidos
            recibo.obtenerConceptosCobrar().forEach { concepto ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = concepto.first, style = MaterialTheme.typography.bodySmall)
                    Text(text = concepto.second.comoMonto(), style = MaterialTheme.typography.bodySmall)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Total a Pagar:", fontWeight = FontWeight.Bold)
                Text(text = recibo.montoTotal.comoMonto(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onCambiarEstado, enabled = !actualizando) {
                    if (pagado) {
                        Icon(Icons.Default.Refresh, contentDescription = "Volver a pendiente", tint = MaterialTheme.colorScheme.primary)
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Marcar como pagado", tint = ExitoVerde)
                    }
                }
                IconButton(onClick = onVerPdf) {
                    Icon(Icons.Default.List, contentDescription = "Ver PDF", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onCompartirPdf) {
                    Icon(Icons.Default.Share, contentDescription = "Compartir", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}