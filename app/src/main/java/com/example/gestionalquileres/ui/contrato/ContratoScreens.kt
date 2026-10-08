package com.example.gestionalquileres.ui.contrato

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gestionalquileres.domain.model.Contrato

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContratoScreen(
    viewModel: ContratoViewModel,
    onVolver: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var mostrarFormulario by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.limpiarMensajes()
        }
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.limpiarMensajes()
            mostrarFormulario = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.cargarDatosIniciales()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (mostrarFormulario) "Nuevo Contrato" else "Gestión de Contratos") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (mostrarFormulario) mostrarFormulario = false else onVolver()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!mostrarFormulario) {
                FloatingActionButton(onClick = { mostrarFormulario = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Crear Contrato")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                if (mostrarFormulario) {
                    FormularioContrato(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                } else {
                    ListaContratosActivos(
                        contratos = uiState.contratosActivos,
                        onFinalizarContrato = { contrato ->
                            viewModel.finalizarContrato(
                                idContrato = contrato.idContrato,
                                inmuebleId = contrato.inmuebleId,
                                unidadId = contrato.unidadId
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ListaContratosActivos(
    contratos: List<Contrato>,
    onFinalizarContrato: (Contrato) -> Unit
) {
    if (contratos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay contratos activos registrados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(contratos) { contrato ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Inquilino: ${contrato.nombreInquilino}", fontWeight = FontWeight.Bold)
                        Text(text = "DNI: ${contrato.dniInquilino}")
                        Spacer(modifier = Modifier.height(6.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Inmueble: ${contrato.direccionInmueble} (${contrato.distritoInmueble})")
                        Text(text = "Unidad: ${contrato.nombreUnidad}")
                        Text(text = "Renta Acordada: $${contrato.precioRentaAcordado}")
                        Text(text = "Vigencia: Desde ${contrato.fechaInicio} hasta ${contrato.fechaFin}")

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onFinalizarContrato(contrato) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Finalizar Contrato")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioContrato(
    uiState: ContratoUiState,
    viewModel: ContratoViewModel
) {
    var fechaInicio by remember { mutableStateOf(uiState.fechaInicio) }
    var fechaFin by remember { mutableStateOf(uiState.fechaFin) }

    var expandInquilino by remember { mutableStateOf(false) }
    var expandUnidad by remember { mutableStateOf(false) }

    // Creamos el estado de desplazamiento
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Evaluación de Solvencia y Asignación", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Regla: El sueldo del inquilino debe ser al menos el doble de la renta.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)

        // 1. Selector de Inquilino
        ExposedDropdownMenuBox(
            expanded = expandInquilino,
            onExpandedChange = { expandInquilino = !expandInquilino }
        ) {
            OutlinedTextField(
                value = uiState.inquilinoSeleccionado?.let { "${it.nombre} (Sueldo: $${it.sueldo})" } ?: "Seleccionar Inquilino",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expandInquilino,
                onDismissRequest = { expandInquilino = false }
            ) {
                uiState.listaInquilinos.forEach { inquilino ->
                    DropdownMenuItem(
                        text = { Text("${inquilino.nombre} - Sueldo: $${inquilino.sueldo}") },
                        onClick = {
                            viewModel.seleccionarInquilino(inquilino)
                            expandInquilino = false
                        }
                    )
                }
            }
        }

        // 2. Selector de Unidad Alquilable
        ExposedDropdownMenuBox(
            expanded = expandUnidad,
            onExpandedChange = { expandUnidad = !expandUnidad }
        ) {
            val textoUnidad = if (uiState.unidadSeleccionada != null && uiState.inmuebleSeleccionado != null) {
                "${uiState.inmuebleSeleccionado?.direccion} - ${uiState.unidadSeleccionada?.nombreFormateado} ($${uiState.unidadSeleccionada?.precioRenta})"
            } else {
                "Seleccionar Unidad Disponible"
            }

            OutlinedTextField(
                value = textoUnidad,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expandUnidad,
                onDismissRequest = { expandUnidad = false }
            ) {
                if (uiState.listaUnidadesDisponibles.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No hay unidades disponibles") },
                        onClick = { expandUnidad = false }
                    )
                } else {
                    uiState.listaUnidadesDisponibles.forEach { (inmueble, unidad) ->
                        DropdownMenuItem(
                            text = { Text("${inmueble.direccion} -> ${unidad.nombreFormateado} ($${unidad.precioRenta})") },
                            onClick = {
                                viewModel.seleccionarUnidad(inmueble, unidad)
                                expandUnidad = false
                            }
                        )
                    }
                }
            }
        }

        // Fechas
        OutlinedTextField(
            value = fechaInicio,
            onValueChange = {
                fechaInicio = it
                viewModel.actualizarFechas(it, fechaFin)
            },
            label = { Text("Fecha de Inicio (Ej. 2026-06-01)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = fechaFin,
            onValueChange = {
                fechaFin = it
                viewModel.actualizarFechas(fechaInicio, it)
            },
            label = { Text("Fecha de Fin (Ej. 2027-06-01)") },
            modifier = Modifier.fillMaxWidth()
        )

        // 3. Resultado de Solvencia
        if (uiState.esSolvente != null) {
            val colorFondo = if (uiState.esSolvente == true) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
            val colorTexto = if (uiState.esSolvente == true) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer

            Card(
                colors = CardDefaults.cardColors(containerColor = colorFondo),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (uiState.esSolvente == true) "¡SOLVENCIA APROBADA!" else "SOLVENCIA RECHAZADA",
                        fontWeight = FontWeight.Bold,
                        color = colorTexto
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = uiState.mensajeSolvencia, color = colorTexto)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.registrarContrato() },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.esSolvente == true
        ) {
            Text("Firmar y Registrar Contrato")
        }
    }
}