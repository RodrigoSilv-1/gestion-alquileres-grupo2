package com.example.gestionalquileres.ui.contrato

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.gestionalquileres.domain.comoMonto
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gestionalquileres.domain.model.Contrato
import com.example.gestionalquileres.ui.common.CampoFecha
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContratoScreen(
    viewModel: ContratoViewModel,
    onVolver: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var mostrarFormulario by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.cargarDatosIniciales() }

    // Muestra errores/éxitos como Snackbar y los limpia (igual que en Inmuebles)
    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        val mensaje = uiState.errorMessage ?: uiState.successMessage
        if (mensaje != null) {
            if (uiState.errorMessage == null) mostrarFormulario = false // éxito: vuelve a la lista
            snackbarHostState.showSnackbar(mensaje)
            viewModel.limpiarMensajes()
        }
    }

    // Con el formulario abierto, "atrás" vuelve a la lista; solo desde la lista sale del módulo
    val volver: () -> Unit = {
        if (mostrarFormulario) mostrarFormulario = false else onVolver()
    }
    BackHandler(onBack = volver)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (mostrarFormulario) "Nuevo Contrato" else "Gestión de Contratos",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = volver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!mostrarFormulario) {
                FloatingActionButton(onClick = {
                    viewModel.prepararNuevoContrato() // formulario siempre en blanco
                    mostrarFormulario = true
                }) {
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
            } else if (mostrarFormulario) {
                FormularioContrato(uiState = uiState, viewModel = viewModel)
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
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Inmueble: ${contrato.direccionInmueble} (${contrato.distritoInmueble})")
                        Text(text = "Unidad: ${contrato.nombreUnidad}")
                        Text(text = "Renta Acordada: ${contrato.precioRentaAcordado.comoMonto()}")
                        if (contrato.ingresoMensualInquilino > 0.0) {
                            Text(text = "Ingreso del inquilino: ${contrato.ingresoMensualInquilino.comoMonto()}")
                        }
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
    // ¡Un solo estado agrupado en lugar de variables sueltas para las fechas!
    var formState by remember {
        mutableStateOf(
            ContratoFormState(
                fechaInicio = uiState.fechaInicio,
                fechaFin = uiState.fechaFin
            )
        )
    }

    var expandInquilino by remember { mutableStateOf(false) }
    var expandUnidad by remember { mutableStateOf(false) }

    val inquilinosDisponibles = remember(uiState.listaInquilinos, uiState.contratosActivos) {
        uiState.inquilinosDisponibles
    }

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
                if (inquilinosDisponibles.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No hay inquilinos sin contrato vigente") },
                        onClick = { expandInquilino = false }
                    )
                } else {
                    inquilinosDisponibles.forEach { inquilino ->
                        DropdownMenuItem(
                            text = { Text("${inquilino.nombre} - Sueldo: ${inquilino.sueldo.comoMonto()}") },
                            onClick = {
                                viewModel.seleccionarInquilino(inquilino)
                                expandInquilino = false
                            }
                        )
                    }
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

        // Fechas con calendario
        val hoy = remember { LocalDate.now() }
        val inicio = runCatching { LocalDate.parse(formState.fechaInicio) }.getOrNull()

        CampoFecha(
            label = "Fecha de inicio",
            fecha = formState.fechaInicio,
            fechaMinima = hoy,
            textoAyuda = "Desde hoy en adelante",
            onFechaSeleccionada = { nuevoInicio ->
                val minimoFin = LocalDate.parse(nuevoInicio).plusMonths(DURACION_MINIMA_MESES)
                // Si el fin ya elegido ya no cumple los 3 meses, se borra
                val finConservado = runCatching { LocalDate.parse(formState.fechaFin) }
                    .getOrNull()
                    ?.takeIf { !it.isBefore(minimoFin) }
                    ?.toString()
                    .orEmpty()

                formState = formState.copy(fechaInicio = nuevoInicio, fechaFin = finConservado)
                viewModel.actualizarFechas(nuevoInicio, finConservado)
            }
        )

        CampoFecha(
            label = "Fecha de fin",
            fecha = formState.fechaFin,
            fechaMinima = inicio?.plusMonths(DURACION_MINIMA_MESES)
                ?: hoy.plusMonths(DURACION_MINIMA_MESES),
            habilitado = inicio != null,
            textoAyuda = if (inicio == null) "Primero elige la fecha de inicio"
            else "Duración mínima: $DURACION_MINIMA_MESES meses",
            onFechaSeleccionada = { nuevoFin ->
                formState = formState.copy(fechaFin = nuevoFin)
                viewModel.actualizarFechas(formState.fechaInicio, nuevoFin)
            }
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
            enabled = uiState.esSolvente == true &&
                    formState.fechaInicio.isNotBlank() &&
                    formState.fechaFin.isNotBlank()
        ) {
            Text("Firmar y Registrar Contrato")
        }
    }
}