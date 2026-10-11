package com.example.gestionalquileres.ui.recibos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gestionalquileres.domain.model.Contrato
import com.example.gestionalquileres.domain.model.Recibo

@Composable
fun ReciboScreen(
    viewModel: ReciboViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.cargarHistorialRecibos()
    }

    // Manejo de mensajes de éxito/error
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            viewModel.mostrarFormularioRegistro(false) // Cierra el formulario al tener éxito
            viewModel.limpiarMensajes()
        }
    }

    when {
        uiState.mostrandoFormularioRegistro -> {
            RegistrarReciboScreen(
                uiState = uiState,
                onCargarContratos = { viewModel.cargarContratosParaCobro() },
                onGuardar = { contrato, agua, luz, ipc ->
                    viewModel.generarRecibo(contrato, agua, luz, ipc)
                },
                onCancelar = { viewModel.mostrarFormularioRegistro(false) }
            )
        }
        else -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onVolver() }
                    ) {
                        IconButton(onClick = onVolver) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                        Text(
                            text = "Gestión de Recibos",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.limpiarMensajes()
                            viewModel.mostrarFormularioRegistro(true)
                        }
                    ) {
                        Text("Nuevo")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buscador
                OutlinedTextField(
                    value = uiState.textoBusqueda,
                    onValueChange = { viewModel.actualizarTextoBusqueda(it) },
                    placeholder = { Text("Buscar recibo por ${uiState.tipoBusqueda}...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar", tint = Color.Gray)
                    },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.LightGray
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Lista de Recibos
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (uiState.recibos.isEmpty()) {
                    Text("No hay recibos generados aún.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.recibos) { recibo ->
                            ReciboCard(recibo = recibo)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarReciboScreen(
    uiState: ReciboUiState,
    onCargarContratos: () -> Unit,
    onGuardar: (Contrato, String, String, String) -> Unit,
    onCancelar: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var contratoSeleccionado by remember { mutableStateOf<Contrato?>(null) }

    var agua by remember { mutableStateOf("") }
    var luz by remember { mutableStateOf("") }
    var ipc by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        onCargarContratos()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .statusBarsPadding()
    ) {
        Text(text = "Generar Nuevo Recibo", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Dropdown para seleccionar a quién cobrar
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = contratoSeleccionado?.let { "${it.nombreInquilino} - ${it.nombreUnidad}" } ?: "Seleccione un contrato vigente",
                onValueChange = {},
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
                            text = { Text("${contrato.nombreInquilino} - ${contrato.nombreUnidad}") },
                            onClick = {
                                contratoSeleccionado = contrato
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
            value = contratoSeleccionado?.precioRentaAcordado?.toString() ?: "0.0",
            onValueChange = {},
            label = { Text("Renta Base Acordada (Fija)") },
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Servicios del mes (Opcional)", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        // Campos variables
        OutlinedTextField(
            value = agua,
            onValueChange = { agua = it },
            label = { Text("Agua") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = luz,
            onValueChange = { luz = it },
            label = { Text("Luz") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = ipc,
            onValueChange = { ipc = it },
            label = { Text("IPC (Ajuste)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        if (uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = uiState.errorMessage, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.weight(1f))

        // Botones de acción
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onCancelar, modifier = Modifier.weight(1f)) {
                Text("Cancelar")
            }
            Button(
                onClick = { onGuardar(contratoSeleccionado!!, agua, luz, ipc) },
                enabled = contratoSeleccionado != null && !uiState.isLoading,
                modifier = Modifier.weight(1f)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Generar y Guardar")
                }
            }
        }
    }
}

@Composable
fun ReciboCard(recibo: Recibo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = recibo.numeroUnico, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(text = recibo.estadoPago, color = if (recibo.estadoPago == "Pagado") Color(0xFF4CAF50) else Color(0xFFF44336))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Fecha Emisión: ${recibo.fechaEmision}", style = MaterialTheme.typography.bodyMedium)

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Usamos la función inteligente del modelo para mostrar solo los conceptos válidos
            recibo.obtenerConceptosCobrar().forEach { concepto ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = concepto.first, style = MaterialTheme.typography.bodySmall)
                    Text(text = "S/ ${concepto.second}", style = MaterialTheme.typography.bodySmall)
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Total a Pagar:", fontWeight = FontWeight.Bold)
                Text(text = "S/ ${recibo.montoTotal}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}