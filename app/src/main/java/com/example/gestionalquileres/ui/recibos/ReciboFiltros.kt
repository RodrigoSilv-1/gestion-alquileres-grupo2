package com.example.gestionalquileres.ui.recibos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.gestionalquileres.domain.model.EstadoPago
import com.example.gestionalquileres.domain.model.etiqueta
import com.example.gestionalquileres.ui.common.CampoFecha
import com.example.gestionalquileres.ui.common.CampoFecha
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltrosRecibos(
    uiState: ReciboUiState,
    onTextoNumero: (String) -> Unit,
    onBuscarNumero: () -> Unit,
    onAbrirContratos: () -> Unit,
    onContrato: (String?) -> Unit,
    onEstado: (EstadoPago?) -> Unit,
    onFechas: (desde: String?, hasta: String?) -> Unit,
    onLimpiar: () -> Unit
) {
    var contratoAbierto by remember { mutableStateOf(false) }
    val filtro = uiState.filtro
    val contratoActual = uiState.contratosActivos.firstOrNull { it.idContrato == filtro.idContrato }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 1) Número exacto (ignora los demás filtros)
        OutlinedTextField(
            value = uiState.textoBusqueda,
            onValueChange = onTextoNumero,
            placeholder = { Text("Número exacto (REC-00001)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.Gray) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onBuscarNumero() }),
            modifier = Modifier.fillMaxWidth()
        )

        // 2) Contrato
        ExposedDropdownMenuBox(
            expanded = contratoAbierto,
            onExpandedChange = {
                contratoAbierto = !contratoAbierto
                if (contratoAbierto) onAbrirContratos()
            }
        ) {
            OutlinedTextField(
                value = contratoActual?.etiqueta() ?: "Todos los contratos",                onValueChange = {},
                readOnly = true,
                label = { Text("Contrato") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = contratoAbierto) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = contratoAbierto, onDismissRequest = { contratoAbierto = false }) {
                DropdownMenuItem(
                    text = { Text("Todos los contratos") },
                    onClick = { onContrato(null); contratoAbierto = false }
                )
                uiState.contratosActivos.forEach { contrato ->
                    DropdownMenuItem(
                        text = { Text(contrato.etiqueta()) },
                        onClick = { onContrato(contrato.idContrato); contratoAbierto = false }
                    )
                }
            }
        }

        // 3) Estado de pago
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf<Pair<String, EstadoPago?>>(
                "Todos" to null,
                "Pendientes" to EstadoPago.PENDIENTE,
                "Pagados" to EstadoPago.PAGADO
            ).forEach { (etiqueta, estado) ->
                FilterChip(
                    selected = filtro.estado == estado,
                    onClick = { onEstado(estado) },
                    label = { Text(etiqueta) }
                )
            }
        }

        // 4) Rango de fechas
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoFecha(
                label = "Desde",
                fecha = filtro.desde.orEmpty(),
                onFechaSeleccionada = { onFechas(it, filtro.hasta) },
                fechaMinima = LocalDate.of(2020, 1, 1),
                fechaMaxima = LocalDate.now(),
                modifier = Modifier.weight(1f)
            )
            CampoFecha(
                label = "Hasta",
                fecha = filtro.hasta.orEmpty(),
                onFechaSeleccionada = { onFechas(filtro.desde, it) },
                fechaMinima = LocalDate.of(2020, 1, 1),
                fechaMaxima = LocalDate.now(),
                modifier = Modifier.weight(1f)
            )
        }

        if (filtro.estaActivo) {
            TextButton(onClick = onLimpiar) { Text("Limpiar filtros") }
        }
    }
}