package com.example.gestionalquileres.ui.recibos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.gestionalquileres.gestionalquileres.ui.common.AppTopBar
import com.example.gestionalquileres.gestionalquileres.ui.common.PieDePaginacion
import com.example.gestionalquileres.util.ReciboPdfAcciones

@Composable
fun ReciboScreen(
    viewModel: ReciboViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val enSubpantalla = uiState.mostrandoFormularioRegistro

    LaunchedEffect(Unit) { viewModel.cargarHistorialRecibos() }

    // Muestra errores/éxitos como Snackbar y los limpia
    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        val mensaje = uiState.errorMessage ?: uiState.successMessage
        if (mensaje != null) {
            snackbarHostState.showSnackbar(mensaje)
            viewModel.limpiarMensajes()
        }
    }

    val volver: () -> Unit = {
        if (enSubpantalla) viewModel.volverAlListado() else onVolver()
    }

    BackHandler(onBack = volver)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                titulo = if (enSubpantalla) "Generar Nuevo Recibo" else "Gestión de Recibos",
                onVolver = volver
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!enSubpantalla) {
                FloatingActionButton(onClick = {
                    viewModel.limpiarMensajes()
                    viewModel.mostrarFormularioRegistro(true)
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo recibo")
                }
            }
        }
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues)) {
            if (enSubpantalla) {
                RegistrarReciboScreen(
                    uiState = uiState,
                    onCargarContratos = viewModel::cargarContratos,
                    onGuardar = viewModel::generarRecibo,
                    onCancelar = viewModel::volverAlListado
                )
            } else {
                ListadoRecibos(uiState = uiState, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun ListadoRecibos(
    uiState: ReciboUiState,
    viewModel: ReciboViewModel
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(start = 16.dp, top = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FiltrosRecibos(
            uiState = uiState,
            onTextoNumero = viewModel::actualizarTextoBusqueda,
            onBuscarNumero = viewModel::buscarPorNumero,
            onAbrirContratos = viewModel::cargarContratos,
            onContrato = viewModel::filtrarPorContrato,
            onEstado = viewModel::filtrarPorEstado,
            onFechas = viewModel::filtrarPorFechas,
            onLimpiar = viewModel::limpiarFiltros
        )

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (uiState.recibos.isEmpty()) {
            Text(if (uiState.filtro.estaActivo) "No hay recibos con esos filtros." else "No hay recibos generados aún.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 88.dp) // evita que el FAB tape la última tarjeta
            ) {
                items(uiState.recibos, key = { it.idRecibo }) { recibo ->
                    ReciboCard(
                        recibo = recibo,
                        actualizando = uiState.idReciboActualizando == recibo.idRecibo,
                        onCambiarEstado = { viewModel.cambiarEstadoPago(recibo) },
                        onVerPdf = { ReciboPdfAcciones.visualizar(context, recibo) },
                        onCompartirPdf = { ReciboPdfAcciones.compartir(context, recibo) }
                    )
                }
                if (uiState.hayMas || uiState.cargandoMas) {
                    item(key = "pie-paginacion") {
                        PieDePaginacion(cargando = uiState.cargandoMas, onCargarMas = viewModel::cargarMasRecibos)
                    }
                }
            }
        }
    }
}