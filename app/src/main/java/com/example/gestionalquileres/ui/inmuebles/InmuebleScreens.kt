package com.example.gestionalquileres.ui.inmuebles

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.gestionalquileres.domain.model.Inmueble
import com.example.gestionalquileres.ui.common.ImagenBase64

// Pantalla principal: decide qué se muestra (listado, formulario, edición o detalle)
@Composable
fun InmuebleScreen(
    viewModel: InmuebleViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val detalle = uiState.inmuebleEnDetalle
    val enEdicion = uiState.inmuebleSeleccionado
    val enSubpantalla = detalle != null || enEdicion != null || uiState.mostrandoFormularioRegistro

    LaunchedEffect(Unit) { viewModel.obtenerTodos() }

    // Muestra errores/éxitos como Snackbar y los limpia
    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        val mensaje = uiState.errorMessage ?: uiState.successMessage
        if (mensaje != null) {
            snackbarHostState.showSnackbar(mensaje)
            viewModel.limpiarMensajes()
        }
    }

    BackHandler { if (enSubpantalla) viewModel.volverAlListado() else onVolver() }

    Box(modifier.fillMaxSize()) {
        when {
            detalle != null -> DetalleInmuebleScreen(
                inmueble = detalle,
                unidades = uiState.unidadesAlquilables,
                cargando = uiState.cargandoUnidades,
                onActualizarPrecio = { unidadId, precio ->
                    viewModel.actualizarPrecioUnidad(detalle.idInmueble, unidadId, precio)
                },
                onVolver = viewModel::volverAlListado
            )

            uiState.mostrandoFormularioRegistro -> RegistrarInmuebleScreen(
                onGuardar = viewModel::registrar,
                onCancelar = viewModel::volverAlListado,
                guardando = uiState.isLoading
            )

            enEdicion != null -> EditarInmuebleScreen(
                inmueble = enEdicion,
                onGuardar = viewModel::actualizar,
                onCancelar = viewModel::volverAlListado,
                guardando = uiState.isLoading
            )

            else -> ListadoInmuebles(
                uiState = uiState,
                onVolver = onVolver,
                onNuevo = { viewModel.mostrarFormularioRegistro(true) },
                onTipoBusqueda = viewModel::actualizarTipoBusqueda,
                onTextoBusqueda = viewModel::actualizarTextoBusqueda,
                onBuscar = viewModel::buscar,
                onMostrarTodos = viewModel::mostrarTodos,
                onVerDetalle = viewModel::verDetalle,
                onModificar = viewModel::seleccionarInmuebleParaEditar,
                onDarDeBaja = viewModel::seleccionarInmuebleParaBaja
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
        )
    }

    // Diálogo: dar de baja
    uiState.inmuebleDarDeBaja?.let { inmueble ->
        AlertDialog(
            onDismissRequest = { viewModel.seleccionarInmuebleParaBaja(null) },
            title = { Text("Dar de baja") },
            text = { Text("¿Deseas dar de baja el inmueble ${inmueble.direccion} ${inmueble.numeroDireccion}?") },
            confirmButton = {
                TextButton(onClick = { viewModel.darDeBaja(inmueble.idInmueble) }) {
                    Text("Sí", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.seleccionarInmuebleParaBaja(null) }) { Text("No") }
            }
        )
    }

    // Diálogo: reactivar un inmueble que estaba en el historial
    uiState.inmuebleInactivoDetectado?.let { inactivo ->
        AlertDialog(
            onDismissRequest = viewModel::cancelarReactivacion,
            title = { Text("Inmueble encontrado en el historial") },
            text = { Text("Este inmueble fue dado de baja anteriormente.\n\n¿Deseas reactivarlo y actualizar sus datos?") },
            confirmButton = {
                TextButton(onClick = { viewModel.reactivarInmueble(inactivo) }) { Text("Reactivar") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelarReactivacion) { Text("Cancelar") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListadoInmuebles(
    uiState: InmuebleUiState,
    onVolver: () -> Unit,
    onNuevo: () -> Unit,
    onTipoBusqueda: (TipoBusqueda) -> Unit,
    onTextoBusqueda: (String) -> Unit,
    onBuscar: () -> Unit,
    onMostrarTodos: () -> Unit,
    onVerDetalle: (Inmueble) -> Unit,
    onModificar: (Inmueble) -> Unit,
    onDarDeBaja: (Inmueble) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Encabezado
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                IconButton(onClick = onVolver) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                }
                Text("Lista de Inmuebles", style = MaterialTheme.typography.headlineMedium)
            }
            Button(onClick = onNuevo) { Text("Nuevo") }
        }

        // Tipo de búsqueda
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TipoBusqueda.values().forEach { tipo ->
                FilterChip(
                    selected = uiState.tipoBusqueda == tipo,
                    onClick = { onTipoBusqueda(tipo) },
                    label = { Text(tipo.etiqueta) }
                )
            }
        }

        // Campo de búsqueda
        OutlinedTextField(
            value = uiState.textoBusqueda,
            onValueChange = onTextoBusqueda,
            placeholder = { Text("Buscar por ${uiState.tipoBusqueda.etiqueta}...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            shape = RoundedCornerShape(24.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onBuscar() }),
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onBuscar) { Text("Buscar") }
            Button(onClick = onMostrarTodos) { Text("Mostrar todos") }
        }

        Spacer(Modifier.size(8.dp))

        // Resultados
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            uiState.inmuebles.isEmpty() -> Text("No hay inmuebles registrados.")

            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.inmuebles, key = { it.idInmueble }) { inmueble ->
                    InmuebleCard(
                        inmueble = inmueble,
                        onModificar = onModificar,
                        onDarDeBaja = onDarDeBaja,
                        onVerDetalle = onVerDetalle
                    )
                }
            }
        }
    }
}

// Tarjeta del inmueble
@Composable
fun InmuebleCard(
    inmueble: Inmueble,
    onModificar: (Inmueble) -> Unit,
    onDarDeBaja: (Inmueble) -> Unit,
    onVerDetalle: (Inmueble) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onVerDetalle(inmueble) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Fila superior: Imagen, Datos principales y un resumen rápido
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Miniatura de la foto o ícono
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    ImagenBase64(inmueble.fotografia, Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                // Textos descriptivos con el modifier weight(1f) bien aplicado
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = inmueble.distrito,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "${inmueble.direccion} ${inmueble.numeroDireccion}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "C.P: ${inmueble.codigoPostal}  •  Pisos: ${if (inmueble.numeroPisos > 0) inmueble.numeroPisos else "S/R"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Línea divisoria sutil opcional o separación para los botones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { onVerDetalle(inmueble) }) {
                    Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF4CAF50))
                    Spacer(Modifier.width(4.dp))
                    Text("Unidades", color = Color(0xFF4CAF50))
                }

                IconButton(onClick = { onModificar(inmueble) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Modificar", tint = MaterialTheme.colorScheme.primary)
                }

                IconButton(onClick = { onDarDeBaja(inmueble) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Dar de baja", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}