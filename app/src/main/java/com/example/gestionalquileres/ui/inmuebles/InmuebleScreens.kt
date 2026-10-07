package com.example.gestionalquileres.ui.inmuebles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gestionalquileres.domain.model.Inmueble
import com.example.gestionalquileres.domain.model.Inquilino
import com.example.gestionalquileres.ui.inquilinos.EditarInquilinoScreen
import com.example.gestionalquileres.ui.inquilinos.InquilinoViewModel
import com.example.gestionalquileres.ui.inquilinos.RegistrarInquilinoScreen
import com.example.gestionalquileres.ui.inquilinos.uriABase64
import java.io.ByteArrayOutputStream

fun uriABase64(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmapOriginal = BitmapFactory.decodeStream(inputStream) ?: return null

        // Redimensionar para no exceder el límite por documento
        val maxDimension = 400f
        val escala = maxDimension / Math.max(bitmapOriginal.width, bitmapOriginal.height).toFloat()
        val anchoFinal = if (escala < 1.0f) (bitmapOriginal.width * escala).toInt() else bitmapOriginal.width
        val altoFinal = if (escala < 1.0f) (bitmapOriginal.height * escala).toInt() else bitmapOriginal.height

        val bitmapReducido = Bitmap.createScaledBitmap(bitmapOriginal, anchoFinal, altoFinal, true)
        val outputStream = ByteArrayOutputStream()
        bitmapReducido.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val bytes = outputStream.toByteArray()

        Base64.encodeToString(bytes, Base64.NO_WRAP)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Pantalla para registrar un nuevo inmueble
@Composable
fun RegistrarInmuebleScreen(
    onGuardar: (Inmueble) -> Unit,
    onCancelar: () -> Unit
) {
    val context = LocalContext.current
    var distrito by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var numeroDireccion by remember { mutableStateOf("") }
    var codigoPostal by remember { mutableStateOf("") }
    var numeroPisos by remember { mutableStateOf("") }
    var unidadesPorPiso by remember { mutableStateOf("") } // NUEVO: Estado para unidades por piso
    var fotografiaBase64 by remember { mutableStateOf<String?>(null) }

    // Selector de fotos nativo de Android
    val selectorFotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            fotografiaBase64 = uriABase64(context, uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Registrar Inmueble",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = distrito,
            onValueChange = { distrito = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Distrito") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = direccion,
            onValueChange = { direccion = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Dirección") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = numeroDireccion,
            onValueChange = { numeroDireccion = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Número de Dirección") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = codigoPostal,
            onValueChange = { codigoPostal = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Código Postal") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = numeroPisos,
            onValueChange = { numeroPisos = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Número de Pisos") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
            )
        )

        // NUEVO: OutlinedTextField para unidades por piso
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = unidadesPorPiso,
            onValueChange = { unidadesPorPiso = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Unidades / Departamentos por Piso") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Fotografía",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Previsualización y selector de imagen
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!fotografiaBase64.isNullOrBlank()) {
                    val bytes = Base64.decode(fotografiaBase64, Base64.DEFAULT)
                    AsyncImage(
                        model = bytes,
                        contentDescription = "Foto inmueble",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = "Sin foto",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        selectorFotoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Text(if (fotografiaBase64 == null) "Elegir foto" else "Cambiar foto")
                }

                if (fotografiaBase64 != null) {
                    TextButton(onClick = { fotografiaBase64 = null }) {
                        Text("Quitar foto", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botones de acción
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancelar,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancelar")
            }

            Button(
                onClick = {
                    val nuevoInmueble = Inmueble(
                        distrito = distrito.trim(),
                        direccion = direccion.trim(),
                        numeroDireccion = numeroDireccion.trim(),
                        codigoPostal = codigoPostal.trim(),
                        numeroPisos = numeroPisos.toIntOrNull() ?: 0,
                        unidadesPorPiso = unidadesPorPiso.toIntOrNull() ?: 0,
                        fotografia = fotografiaBase64
                    )
                    onGuardar(nuevoInmueble)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Guardar")
            }
        }
    }
}

// Pantalla para actualizar datos de un inmueble
@Composable
fun EditarInmuebleScreen(
    inmueble: Inmueble,
    onGuardar: (Inmueble) -> Unit,
    onCancelar: () -> Unit
) {
    val context = LocalContext.current
    var distrito by remember { mutableStateOf(inmueble.distrito) }
    var direccion by remember { mutableStateOf(inmueble.direccion) }
    var numeroDireccion by remember { mutableStateOf(inmueble.numeroDireccion) }
    var codigoPostal by remember { mutableStateOf(inmueble.codigoPostal) }
    var numeroPisos by remember { mutableStateOf(inmueble.numeroPisos.toString()) }
    var unidadesPorPiso by remember { mutableStateOf(inmueble.unidadesPorPiso.toString()) } // NUEVO
    var fotografiaBase64 by remember { mutableStateOf(inmueble.fotografia) }

    val selectorFotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            fotografiaBase64 = uriABase64(context, uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Modificar Inmueble",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = distrito,
            onValueChange = { distrito = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Distrito") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = direccion,
            onValueChange = { direccion = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Dirección") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = numeroDireccion,
            onValueChange = { numeroDireccion = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Número de Dirección") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = codigoPostal,
            onValueChange = { codigoPostal = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Código Postal") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = numeroPisos,
            onValueChange = { numeroPisos = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            label = { Text("Número de Pisos") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
            )
        )

        // NUEVO: Campo para editar unidades por piso
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = unidadesPorPiso,
            onValueChange = { unidadesPorPiso = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            label = { Text("Unidades / Departamentos por Piso") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Fotografía",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!fotografiaBase64.isNullOrBlank()) {
                    val bytes = Base64.decode(fotografiaBase64, Base64.DEFAULT)
                    AsyncImage(
                        model = bytes,
                        contentDescription = "Foto inmueble",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = "Sin foto",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        selectorFotoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Text(if (fotografiaBase64 == null) "Elegir foto" else "Cambiar foto")
                }

                if (fotografiaBase64 != null) {
                    TextButton(onClick = { fotografiaBase64 = null }) {
                        Text("Quitar foto", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancelar,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancelar")
            }

            Button(
                onClick = {
                    val inmuebleActualizado = inmueble.copy(
                        distrito = distrito.trim(),
                        direccion = direccion.trim(),
                        numeroDireccion = numeroDireccion.trim(),
                        codigoPostal = codigoPostal.trim(),
                        numeroPisos = numeroPisos.toIntOrNull() ?: inmueble.numeroPisos,
                        unidadesPorPiso = unidadesPorPiso.toIntOrNull() ?: inmueble.unidadesPorPiso,
                        fotografia = fotografiaBase64
                    )
                    onGuardar(inmuebleActualizado)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Guardar cambios")
            }
        }
    }
}

@Composable
fun DetalleInmuebleScreen(
    inmueble: Inmueble,
    viewModel: InmuebleViewModel,
    onVolver: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Estados locales para controlar el diálogo de edición de precio
    var unidadSeleccionadaParaEditar by remember { mutableStateOf<com.example.gestionalquileres.domain.model.UnidadAlquilable?>(null) }
    var nuevoPrecioTexto by remember { mutableStateOf("") }

    // Cargar las unidades al abrir la pantalla
    LaunchedEffect(inmueble.idInmueble) {
        viewModel.obtenerUnidadesPorInmueble(inmueble.idInmueble)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Barra superior
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onVolver) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver")
            }
            Column {
                Text(
                    text = inmueble.distrito,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "${inmueble.direccion} ${inmueble.numeroDireccion}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Unidades Alquilables",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Listado de unidades
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.unidadesAlquilables.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay unidades registradas para este inmueble.")
                }
            }
            else -> {
                val unidadesOrdenadas = uiState.unidadesAlquilables.sortedWith(
                    compareBy({ it.piso }, { it.numeroUnidad })
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(unidadesOrdenadas) { unidad ->
                        val textoEstado = if (!unidad.estaOcupada) "Disponible" else "No Disponible"
                        val colorSuperficie = if (!unidad.estaOcupada)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer

                        // MODIFICACIÓN: Hacemos la Card interactiva con clickable para editar el precio
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    unidadSeleccionadaParaEditar = unidad
                                    // Precarga el precio actual, si es mayor a 0, si no dejamos vacío
                                    nuevoPrecioTexto = if (unidad.precioRenta > 0.0) unidad.precioRenta.toString() else ""
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = unidad.nombreFormateado.ifBlank { "Unidad ${unidad.numeroUnidad}" },
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Piso: ${unidad.piso} - Unidad: ${unidad.numeroUnidad}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // NUEVO: Muestra el precio de la renta en la tarjeta
                                    Text(
                                        text = "Renta: $${unidad.precioRenta}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = colorSuperficie
                                ) {
                                    Text(
                                        text = textoEstado,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // NUEVO: Diálogo emergente para editar el precio de la unidad seleccionada
    if (unidadSeleccionadaParaEditar != null) {
        AlertDialog(
            onDismissRequest = { unidadSeleccionadaParaEditar = null },
            title = { Text("Editar Precio de Renta") },
            text = {
                Column {
                    Text("Asigna o modifica el precio mensual para la ${unidadSeleccionadaParaEditar?.nombreFormateado}:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = nuevoPrecioTexto,
                        onValueChange = { nuevoPrecioTexto = it },
                        label = { Text("Precio de Renta ($)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val precioConvertido = nuevoPrecioTexto.toDoubleOrNull() ?: 0.0
                        // Llamamos al ViewModel que creamos previamente para actualizar Firestore
                        viewModel.actualizarPrecioUnidad(
                            inmuebleId = inmueble.idInmueble,
                            unidadId = unidadSeleccionadaParaEditar!!.id,
                            nuevoPrecio = precioConvertido
                        )
                        unidadSeleccionadaParaEditar = null
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { unidadSeleccionadaParaEditar = null }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// Pantalla principal: listado, búsquedas y control de navegación
@Composable
fun InmuebleScreen(
    viewModel: InmuebleViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Carga la lista inicial desde la base de datos
    LaunchedEffect(Unit) {
        viewModel.obtenerTodos()
    }

    var inmuebleAEditar by remember { mutableStateOf<Inmueble?>(null) }
    var mostrandoFormularioRegistro by remember { mutableStateOf(false) }
    var inmuebleSeleccionadoDetalle by remember { mutableStateOf<Inmueble?>(null) }
    var tipoBusqueda by remember { mutableStateOf("Distrito") }
    var textoBusqueda by remember { mutableStateOf("") }

    when {
        inmuebleSeleccionadoDetalle != null -> {
            DetalleInmuebleScreen(
                inmueble = inmuebleSeleccionadoDetalle!!,
                viewModel = viewModel,
                onVolver = { inmuebleSeleccionadoDetalle = null }
            )
        }
        mostrandoFormularioRegistro -> {
            RegistrarInmuebleScreen(
                onGuardar = { nuevoInmueble ->
                    viewModel.registrar(nuevoInmueble)
                },
                onCancelar = {
                    mostrandoFormularioRegistro = false
                }
            )

            // Cierra el formulario cuando la operación se guarda correctamente
            LaunchedEffect(uiState.successMessage) {
                if (uiState.successMessage != null) {
                    mostrandoFormularioRegistro = false
                    viewModel.limpiarMensajes()
                }
            }
        }

        inmuebleAEditar != null -> {
            EditarInmuebleScreen(
                inmueble = inmuebleAEditar!!,
                onGuardar = { actualizado ->
                    viewModel.actualizar(actualizado)
                    inmuebleAEditar = null
                },
                onCancelar = {
                    inmuebleAEditar = null
                }
            )
        }

        else -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                // Barra superior: volver y botón para nuevo registro
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clickable { onVolver() }
                    ) {
                        IconButton(onClick = onVolver) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Volver"
                            )
                        }
                        Text(
                            text = "Lista de Inmuebles",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.limpiarMensajes()
                            mostrandoFormularioRegistro = true
                        }
                    ) {
                        Text("Nuevo")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Filtro para elegir criterio de búsqueda
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { tipoBusqueda = "Distrito" }) {
                        Text("Distrito")
                    }
                    Button(onClick = { tipoBusqueda = "Código Postal" }) {
                        Text("Código Postal")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = textoBusqueda,
                    onValueChange = { textoBusqueda = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar por $tipoBusqueda") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (textoBusqueda.isNotBlank()) {
                                if (tipoBusqueda == "Distrito") {
                                    viewModel.obtenerPorDistrito(textoBusqueda)
                                } else {
                                    viewModel.obtenerPorCodigoPostal(textoBusqueda)
                                }
                            }
                        }
                    ) {
                        Text("Buscar")
                    }

                    Button(
                        onClick = {
                            textoBusqueda = ""
                            viewModel.obtenerTodos()
                        }
                    ) {
                        Text("Mostrar todos")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Listado de inmuebles activos
                if (uiState.isLoading) {
                    Text("Cargando lista de inmuebles...")
                } else if (uiState.inmuebles.isEmpty()) {
                    Text("No hay inmuebles registrados.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.inmuebles) { inmueble ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Muestra foto si tiene, o círculo con un ícono de casa, haciendo referencia a inmueble
                                    if (!inmueble.fotografia.isNullOrBlank()) {
                                        val bytes = remember(inmueble.fotografia) {
                                            Base64.decode(inmueble.fotografia, Base64.DEFAULT)
                                        }
                                        AsyncImage(
                                            model = bytes,
                                            contentDescription = "Foto de ${inmueble.idInmueble}",
                                            modifier = Modifier
                                                .height(100.dp)
                                                .width(55.dp)
                                                .clickable{
                                                    inmuebleSeleccionadoDetalle = inmueble
                                                }
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .height(100.dp)
                                                .width(55.dp)
                                                .clickable{
                                                    inmuebleSeleccionadoDetalle = inmueble
                                                }
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Home,
                                                contentDescription = "Sin fotografía",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }

                                    // Datos principales y botones de gestión
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = inmueble.distrito,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "Dirección: ${inmueble.direccion} ${inmueble.numeroDireccion}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Código Postal: ${inmueble.codigoPostal}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = if (inmueble.numeroPisos > 0)
                                                "# de pisos: ${inmueble.numeroPisos}"
                                            else
                                                "# de pisos: Sin registrar",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (inmueble.unidadesPorPiso > 0)
                                                "Unidades por piso: ${inmueble.unidadesPorPiso}"
                                            else
                                                "Unidades por piso: Sin registrar",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(onClick = { inmuebleAEditar = inmueble }) {
                                                Text("Modificar")
                                            }

                                            var inmuebleDarDeBaja by remember {
                                                mutableStateOf<Inmueble?>(null)
                                            }

                                            OutlinedButton(onClick = { inmuebleDarDeBaja = inmueble }) {
                                                Text("Dar de baja")
                                            }

                                            // Confirmación antes de aplicar la baja lógica
                                            if (inmuebleDarDeBaja != null) {
                                                AlertDialog(
                                                    onDismissRequest = { inmuebleDarDeBaja = null },
                                                    title = { Text("Dar de baja") },
                                                    text = { Text("¿Deseas dar de baja el inmueble seleccionado?") },
                                                    confirmButton = {
                                                        TextButton(
                                                            onClick = {
                                                                viewModel.darDeBaja(inmuebleDarDeBaja!!.idInmueble)
                                                                inmuebleDarDeBaja = null
                                                            }
                                                        ) {
                                                            Text("Sí")
                                                        }
                                                    },
                                                    dismissButton = {
                                                        TextButton(onClick = { inmuebleDarDeBaja = null }) {
                                                            Text("No")
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}