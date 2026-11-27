package com.example.gestionalquileres.ui.contrato

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.gestionalquileres.ui.common.PieDePaginacion
import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gestionalquileres.domain.model.Contrato

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContratoScreen(
    viewModel: ContratoViewModel,
    onVolver: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var mostrarFormulario by rememberSaveable { mutableStateOf(false) }
    var idContratoPorFinalizar by rememberSaveable { mutableStateOf<String?>(null) }

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

    // Confirmación antes de finalizar: libera la unidad y no se puede deshacer
    val contratoPorFinalizar = uiState.contratosActivos.firstOrNull { it.idContrato == idContratoPorFinalizar }
    if (contratoPorFinalizar != null) {
        AlertDialog(
            onDismissRequest = { idContratoPorFinalizar = null },
            title = { Text("Finalizar contrato") },
            text = {
                Text(
                    "¿Finalizar el contrato de ${contratoPorFinalizar.nombreInquilino} en la unidad " +
                            "${contratoPorFinalizar.nombreUnidad} (${contratoPorFinalizar.direccionInmueble})?\n\n" +
                            "La unidad quedará disponible para un nuevo contrato. " +
                            "Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    idContratoPorFinalizar = null
                    viewModel.finalizarContrato(
                        idContrato = contratoPorFinalizar.idContrato,
                        inmuebleId = contratoPorFinalizar.inmuebleId,
                        unidadId = contratoPorFinalizar.unidadId
                    )
                }) { Text("Finalizar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { idContratoPorFinalizar = null }) { Text("Cancelar") }
            }
        )
    }

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
                    hayMas = uiState.hayMas,
                    cargandoMas = uiState.cargandoMas,
                    onCargarMas = viewModel::cargarMasContratos,
                    onFinalizarContrato = { contrato -> idContratoPorFinalizar = contrato.idContrato }
                )
            }
        }
    }
}

@Composable
fun ListaContratosActivos(
    contratos: List<Contrato>,
    hayMas: Boolean,
    cargandoMas: Boolean,
    onCargarMas: () -> Unit,
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
            items(contratos, key = { it.idContrato }) { contrato ->
                ContratoCard(contrato = contrato, onFinalizar = onFinalizarContrato)
            }
            if (hayMas || cargandoMas) {
                item(key = "pie-paginacion") {
                    PieDePaginacion(cargando = cargandoMas, onCargarMas = onCargarMas)
                }
            }
        }
    }
}