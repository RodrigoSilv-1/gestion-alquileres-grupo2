package com.example.gestionalquileres.ui.inmuebles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gestionalquileres.domain.model.Inmueble
import com.example.gestionalquileres.domain.model.UnidadAlquilable

@Composable
fun DetalleInmuebleScreen(
    inmueble: Inmueble,
    unidades: List<UnidadAlquilable>,
    cargando: Boolean,
    onActualizarPrecio: (unidadId: String, precio: Double) -> Unit,
    onVolver: () -> Unit
) {
    var unidadEnEdicion by remember { mutableStateOf<UnidadAlquilable?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onVolver) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
            }
            Column {
                Text(inmueble.distrito, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "${inmueble.direccion} ${inmueble.numeroDireccion}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text("Unidades Alquilables", style = MaterialTheme.typography.titleMedium)

        when {
            cargando -> CajaCentrada { CircularProgressIndicator() }
            unidades.isEmpty() -> CajaCentrada { Text("No hay unidades registradas para este inmueble.") }
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(unidades, key = { it.id }) { unidad ->
                    UnidadCard(unidad = unidad, onClick = { unidadEnEdicion = unidad })
                }
            }
        }
    }

    unidadEnEdicion?.let { unidad ->
        EditarPrecioDialog(
            unidad = unidad,
            onConfirmar = { precio ->
                onActualizarPrecio(unidad.id, precio)
                unidadEnEdicion = null
            },
            onDismiss = { unidadEnEdicion = null }
        )
    }
}

@Composable
private fun UnidadCard(unidad: UnidadAlquilable, onClick: () -> Unit) {
    val disponible = !unidad.estaOcupada
    val colorEstado = if (disponible) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.errorContainer

    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = unidad.nombreFormateado.ifBlank { "Unidad ${unidad.numeroUnidad}" },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Piso: ${unidad.piso} - Unidad: ${unidad.numeroUnidad}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Renta: $${"%.2f".format(unidad.precioRenta)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Surface(shape = MaterialTheme.shapes.small, color = colorEstado) {
                Text(
                    text = if (disponible) "Disponible" else "No Disponible",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun EditarPrecioDialog(
    unidad: UnidadAlquilable,
    onConfirmar: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var texto by remember(unidad.id) {
        mutableStateOf(if (unidad.precioRenta > 0.0) unidad.precioRenta.toString() else "")
    }
    val precio = texto.replace(',', '.').toDoubleOrNull()
    val valido = precio != null && precio >= 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Precio de Renta") },
        text = {
            Column {
                Text("Asigna o modifica el precio mensual de la unidad ${unidad.nombreFormateado}:")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    label = { Text("Precio de Renta ($)") },
                    singleLine = true,
                    isError = texto.isNotBlank() && !valido,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { precio?.let(onConfirmar) }, enabled = valido) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun CajaCentrada(contenido: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { contenido() }
}