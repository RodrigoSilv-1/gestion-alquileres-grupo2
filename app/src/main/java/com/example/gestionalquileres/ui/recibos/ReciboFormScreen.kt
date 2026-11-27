package com.example.gestionalquileres.ui.recibos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gestionalquileres.domain.comoMonto
import com.example.gestionalquileres.domain.limpiarMonto
import com.example.gestionalquileres.domain.model.Contrato
import com.example.gestionalquileres.domain.model.etiqueta
import com.example.gestionalquileres.ui.common.CampoTexto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarReciboScreen(
    uiState: ReciboUiState,
    onCargarContratos: () -> Unit,
    onGuardar: (Contrato, String, String, String) -> Unit,
    onCancelar: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // ¡Un solo estado agrupado en lugar de 4 variables sueltas!
    var formState by remember { mutableStateOf(ReciboFormState()) }

    LaunchedEffect(Unit) {
        onCargarContratos()
    }

    val guardar: () -> Unit = {
        formState.contratoSeleccionado?.let { contrato ->
            if (!uiState.isLoading) onGuardar(contrato, formState.agua, formState.luz, formState.mantenimiento)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Dropdown para seleccionar a quién cobrar
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = formState.contratoSeleccionado?.etiqueta() ?: "Seleccione un contrato vigente",                onValueChange = {},
                readOnly = true,
                label = { Text("Contrato (Inquilino / Unidad)") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (uiState.contratosActivos.isEmpty()) {
                    DropdownMenuItem(text = { Text("No hay contratos vigentes") }, onClick = { expanded = false })
                } else {
                    uiState.contratosActivos.forEach { contrato ->
                        DropdownMenuItem(
                            text = { Text(contrato.etiqueta()) },
                            onClick = {
                                formState = formState.copy(contratoSeleccionado = contrato)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Campo de Renta Base (Bloqueado)
        OutlinedTextField(
            value = (formState.contratoSeleccionado?.precioRentaAcordado ?: 0.0).comoMonto(),
            onValueChange = {},
            label = { Text("Renta Base Acordada (Fija)") },
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Servicios del mes (Opcional)", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        // Campos variables utilizando el estado unificado con `.copy(...)`
        CampoTexto(
            formState.agua, { formState = formState.copy(agua = limpiarMonto(it)) },
            "Agua", KeyboardType.Decimal
        )
        Spacer(modifier = Modifier.height(8.dp))
        CampoTexto(
            formState.luz, { formState = formState.copy(luz = limpiarMonto(it)) },
            "Luz", KeyboardType.Decimal
        )
        Spacer(modifier = Modifier.height(8.dp))
        CampoTexto(
            formState.mantenimiento, { formState = formState.copy(mantenimiento = limpiarMonto(it)) },
            "IPC (Ajuste)", KeyboardType.Decimal,
            imeAction = ImeAction.Done,
            onDone = guardar
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Botones de acción
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onCancelar, enabled = !uiState.isLoading, modifier = Modifier.weight(1f)) {
                Text("Cancelar")
            }
            Button(
                onClick = guardar,
                enabled = formState.contratoSeleccionado != null && !uiState.isLoading,
                modifier = Modifier.weight(1f)
            ) {
                if (uiState.isLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Guardar")
            }
        }
    }
}