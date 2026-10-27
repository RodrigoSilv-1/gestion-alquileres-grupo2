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
import androidx.compose.ui.platform.LocalContext
import com.example.gestionalquileres.util.ReciboPdfAcciones

@Composable
fun ReciboScreen(
    viewModel: ReciboViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    // esta variable conecta el vm con la interfaz, permite que ui este pendiente
    // de cualquier cambio en los datos y actualizarse mostrando la info correcta
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current

// Se ejecuta este componente cuando la pantalla entra por primera vez
    LaunchedEffect(Unit) {
        // Dispara la carga inicial de la lista/historial de recibos desde el ViewModel.
        viewModel.cargarHistorialRecibos()
    }

// Escucha específicamente los cambios en la propiedad 'successMessage' del estado.
    LaunchedEffect(uiState.successMessage) {
        // Si la propiedad contiene un mensaje de éxito (no es nulo)
        if (uiState.successMessage != null) {
            // Cierra el formulario de registro automáticamente
            viewModel.mostrarFormularioRegistro(false)
            // Limpia el mensaje en el ViewModel para evitar ejecuciones repetidas
            viewModel.limpiarMensajes()
        }
    }

    when {
        uiState.mostrandoFormularioRegistro -> {
            RegistrarReciboScreen(
                uiState = uiState,
                onCargarContratos = { viewModel.cargarContratosParaCobro() },
                onGuardar = { contrato, agua, luz, mantenimiento ->
                    viewModel.generarRecibo(contrato, agua, luz, mantenimiento)
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
                        modifier = Modifier
                            .weight(1f) // Permite que el título ocupe el espacio necesario
                            .clickable { onVolver() }
                    ) {
                        IconButton(onClick = onVolver) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                        Text(
                            text = "Gestión de Recibos",
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 2 // Permite que baje a la segunda línea si no entra horizontalmente
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

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
                // Contenedor horizontal (Row) que ocupa todo el ancho disponible de la pantalla y separa sus elementos internos por 8dp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Creamos una lista con las opciones de filtros disponibles obtenidas del estado
                    val filtros = listOf(
                        ReciboUiState.FILTRO_TODOS,
                        ReciboUiState.FILTRO_PENDIENTES,
                        ReciboUiState.FILTRO_COBRADOS
                    )

                    // 2. Iteramos sobre cada elemento de la lista para generar dinámicamente un chip de filtro
                    filtros.forEach { filtro ->
                        FilterChip(
                            // El chip se marca visualmente como seleccionado si coincide con el filtro actual del ViewModel
                            selected = uiState.filtroEstado == filtro,

                            // Al hacer clic en el chip, se le comunica al ViewModel que actualice el filtro activo
                            onClick = {
                                viewModel.actualizarFiltroEstado(filtro)
                            },

                            // Contenido visual del chip, que muestra el texto correspondiente al filtro
                            label = {
                                Text(filtro)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Lista de Recibos
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (uiState.recibosFiltrados.isEmpty()) {
                    Text(
                        text = if (uiState.recibos.isEmpty()) {
                            "No hay recibos generados aún."
                        } else {
                            "No hay recibos que coincidan con el filtro."
                        }
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.recibosFiltrados,
                            key = { recibo -> recibo.idRecibo }
                        ) { recibo ->
                            ReciboCard(
                                recibo = recibo,
                                onCambiarEstado = {
                                    viewModel.cambiarEstadoPago(recibo)
                                },
                                onVerPdf = {
                                    ReciboPdfAcciones.visualizar(
                                        context = context,
                                        recibo = recibo
                                    )
                                },
                                onCompartirPdf = {
                                    ReciboPdfAcciones.compartir(
                                        context = context,
                                        recibo = recibo
                                    )
                                }
                            )
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
    // Creamos un estado booleano para controlar si un menú desplegable
    var expanded by remember { mutableStateOf(false) }
// Agrupamos todos los campos del formulario en un solo objeto (ReciboFormState)
// en lugar de declarar variables separadas para el agua, la luz, el importe, etc.,
    var formState by remember { mutableStateOf(ReciboFormState()) }

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
            // Creamos una caja de texto con borde que actúa como botón desplegable
            OutlinedTextField(
                // Mostramos el nombre del inquilino y unidad si ya se eligió uno; si está vacío, mostramos un texto de guía
                value = formState.contratoSeleccionado?.let { "${it.nombreInquilino} -${it.nombreUnidad}" } ?: "Seleccione un contrato vigente",
                // Está vacío porque el usuario no escribe aquí, solo selecciona haciendo clic
                onValueChange = {},
                // Lo ponemos en modo solo lectura para evitar que se abra el teclado del celular
                readOnly = true,
                // El título o etiqueta que flota sobre la caja
                label = { Text("Contrato (Inquilino / Unidad)") },
                // Coloca la flechita a la derecha que indica si el menú está abierto o cerrado
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                // Hace que ocupe todo el ancho de la pantalla y se conecte con el menú desplegable
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            // El menú flotante que se despliega al hacer clic en la caja anterior
            ExposedDropdownMenu(
                expanded = expanded, // Controla si el menú está visible (true) o oculto (false)
                onDismissRequest = { expanded = false } // Si el usuario toca afuera, el menú se cierra
            ) {
                // Si la lista de contratos activos está vacía
                if (uiState.contratosActivos.isEmpty()) {
                    // Mostramos un mensaje de aviso y cerramos el menú si lo presionan
                    DropdownMenuItem(text = { Text("No hay contratos vigentes") }, onClick = { expanded = false })
                } else {
                    // Si hay contratos, recorremos la lista uno por uno para mostrarlos en opciones
                    uiState.contratosActivos.forEach { contrato ->
                        DropdownMenuItem(
                            // Mostramos el nombre del inquilino y su unidad en cada opción del menú
                            text = { Text("${contrato.nombreInquilino} -${contrato.nombreUnidad}") },
                            onClick = {
                                // Guardamos el contrato que el usuario eligió dentro del estado del formulario
                                formState = formState.copy(contratoSeleccionado = contrato)
                                // Cerramos el menú automáticamente después de seleccionar
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
            value = formState.contratoSeleccionado?.precioRentaAcordado?.toString() ?: "0.0",
            onValueChange = {},
            label = { Text("Renta Base Acordada (Fija)") },
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Servicios del mes (Opcional)", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        // Campos variables utilizando el estado unificado con `.copy(...)`
        OutlinedTextField(
            value = formState.agua,
            onValueChange = { formState = formState.copy(agua = it) },
            label = { Text("Agua") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = formState.luz,
            onValueChange = { formState = formState.copy(luz = it) },
            label = { Text("Luz") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = formState.mantenimiento,
            onValueChange = { formState = formState.copy(mantenimiento = it) },
            label = { Text("Mantenimiento") },
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
            // Componente Botón de Jetpack Compose para realizar la acción principal del formulario
            Button(
                onClick = {
                    // 1. Verificamos de forma segura si hay un contrato seleccionado en el estado del formulario
                    formState.contratoSeleccionado?.let { contrato ->
                        // Si existe, ejecutamos la función de guardado pasando el contrato y los servicios (agua, luz, mantenimiento)
                        onGuardar(contrato, formState.agua, formState.luz, formState.mantenimiento)
                    }
                },
                // 2. El botón solo se habilita si hay un contrato seleccionado Y la interfaz no se encuentra en estado de carga (isLoading)
                enabled = formState.contratoSeleccionado != null && !uiState.isLoading,
                // 3. Modificador de diseño que distribuye el espacio disponible de manera equitativa (peso de 1)
                modifier = Modifier.weight(1f)
            ) {
                // 4. Cambiamos el contenido visual del botón de manera dinámica según el estado de carga
                if (uiState.isLoading) {
                    // Si está cargando, mostramos un indicador de progreso circular en miniatura con color blanco
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    // Si no está cargando, mostramos el texto tradicional del botón
                    Text("Generar y Guardar")
                }
            }
        }
    }
}

@Composable
fun ReciboCard(
    recibo: Recibo,
    onCambiarEstado: () -> Unit,
    onVerPdf: () -> Unit,
    onCompartirPdf: () -> Unit
) {
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
                Text(
                    text = recibo.estadoPago,
                    color = if (recibo.estadoPago == Recibo.ESTADO_COBRADO) {
                        Color(0xFF4CAF50)
                    } else {
                        Color(0xFFF44336)
                    },
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Fecha Emisión: ${recibo.fechaEmision}", style = MaterialTheme.typography.bodyMedium)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Muestra solo los conceptos válidos
            recibo.obtenerConceptosCobrar().forEach { concepto ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = concepto.first, style = MaterialTheme.typography.bodySmall)
                    Text(text = "S/ ${concepto.second}", style = MaterialTheme.typography.bodySmall)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Total a Pagar:", fontWeight = FontWeight.Bold)
                Text(text = "S/ ${recibo.montoTotal}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onCambiarEstado,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (recibo.estadoPago == Recibo.ESTADO_COBRADO) {
                        "Volver a pendiente"
                    } else {
                        "Marcar como cobrado"
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onVerPdf,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ver PDF")
                }

                OutlinedButton(
                    onClick = onCompartirPdf,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Compartir")
                }
            }
        }
    }
}