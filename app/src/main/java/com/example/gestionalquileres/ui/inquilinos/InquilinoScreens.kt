package com.example.gestionalquileres.ui.inquilinos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.gestionalquileres.domain.model.Inquilino
import com.example.gestionalquileres.ui.common.AppTopBar

// Pantalla principal: decide qué se muestra (listado, registro o edición)
@Composable
fun InquilinoScreen(
    viewModel: InquilinoViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val enEdicion = uiState.inquilinoSeleccionado
    val enSubpantalla = enEdicion != null || uiState.mostrandoFormularioRegistro

    LaunchedEffect(Unit) { viewModel.obtenerTodos() }

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
                titulo = when {
                    uiState.mostrandoFormularioRegistro -> "Registrar Inquilino"
                    enEdicion != null -> "Modificar Inquilino"
                    else -> "Lista de Inquilinos"
                },
                onVolver = volver
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!enSubpantalla) {
                FloatingActionButton(onClick = { viewModel.mostrarFormularioRegistro(true) }) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo inquilino")
                }
            }
        }
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                uiState.mostrandoFormularioRegistro -> RegistrarInquilinoScreen(
                    onGuardar = viewModel::registrar,
                    onCancelar = viewModel::volverAlListado,
                    guardando = uiState.isLoading
                )

                enEdicion != null -> EditarInquilinoScreen(
                    inquilino = enEdicion,
                    onGuardar = viewModel::actualizar,
                    onCancelar = viewModel::volverAlListado,
                    guardando = uiState.isLoading
                )

                else -> ListadoInquilinos(
                    uiState = uiState,
                    onTipoBusqueda = viewModel::actualizarTipoBusqueda,
                    onTextoBusqueda = viewModel::actualizarTextoBusqueda,
                    onBuscar = viewModel::buscar,
                    onMostrarTodos = viewModel::mostrarTodos,
                    onModificar = viewModel::seleccionarInquilinoParaEditar,
                    onDarDeBaja = viewModel::seleccionarInquilinoParaBaja
                )
            }
        }
    }

    // Alerta: DNI de un inquilino dado de baja anteriormente
    uiState.inquilinoInactivoDetectado?.let { inactivo ->
        AlertDialog(
            onDismissRequest = viewModel::cancelarReactivacion,
            title = { Text("Inquilino encontrado en el historial") },
            text = {
                Text(
                    "El DNI ${inactivo.dni} pertenece a un inquilino dado de baja anteriormente.\n\n" +
                            "¿Deseas reactivar su expediente y actualizar sus datos?"
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.reactivarInquilino(inactivo) }) { Text("Reactivar") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelarReactivacion) { Text("Cancelar") }
            }
        )
    }

    // Alerta: confirmar baja
    uiState.inquilinoDarDeBaja?.let { aBaja ->
        AlertDialog(
            onDismissRequest = { viewModel.seleccionarInquilinoParaBaja(null) },
            title = { Text("Dar de baja") },
            text = { Text("¿Deseas dar de baja a ${aBaja.nombre}?") },
            confirmButton = {
                TextButton(onClick = { viewModel.darDeBaja(aBaja.idInquilino) }) {
                    Text("Sí", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.seleccionarInquilinoParaBaja(null) }) { Text("No") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListadoInquilinos(
    uiState: InquilinoUiState,
    onTipoBusqueda: (TipoBusquedaInquilino) -> Unit,
    onTextoBusqueda: (String) -> Unit,
    onBuscar: () -> Unit,
    onMostrarTodos: () -> Unit,
    onModificar: (Inquilino) -> Unit,
    onDarDeBaja: (Inquilino) -> Unit
) {
    val visibles = uiState.inquilinosVisibles

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TipoBusquedaInquilino.values().forEach { tipo ->
                FilterChip(
                    selected = uiState.tipoBusqueda == tipo,
                    onClick = { onTipoBusqueda(tipo) },
                    label = { Text(tipo.etiqueta) }
                )
            }
        }

        OutlinedTextField(
            value = uiState.textoBusqueda,
            onValueChange = onTextoBusqueda,
            placeholder = { Text("Buscar por ${uiState.tipoBusqueda.etiqueta}...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onBuscar() }),
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onBuscar) { Text("Buscar") }
            OutlinedButton(onClick = onMostrarTodos) { Text("Mostrar todos") }
        }

        when {
            uiState.isLoading && uiState.inquilinos.isEmpty() ->
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

            visibles.isEmpty() -> Text(
                if (uiState.filtroAplicado.isBlank()) "No hay inquilinos registrados."
                else "Sin resultados para \"${uiState.filtroAplicado}\"."
            )

            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visibles, key = { it.idInquilino }) { inquilino ->
                    InquilinoCard(
                        inquilino = inquilino,
                        onModificar = onModificar,
                        onDarDeBaja = onDarDeBaja
                    )
                }
            }
        }
    }
}