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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gestionalquileres.domain.model.Inmueble
import java.io.ByteArrayOutputStream
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

fun uriABase64(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmapOriginal = BitmapFactory.decodeStream(inputStream) ?: return null

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
    var fotografiaBase64 by remember { mutableStateOf<String?>(null) }

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
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Distrito") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = direccion,
            onValueChange = { direccion = it },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Dirección") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = numeroDireccion,
            onValueChange = { numeroDireccion = it },
            shape = RoundedCornerShape(12.dp),
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
            shape = RoundedCornerShape(12.dp),
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
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Número de Pisos") },
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
                    val nuevoInmueble = Inmueble(
                        distrito = distrito.trim(),
                        direccion = direccion.trim(),
                        numeroDireccion = numeroDireccion.trim(),
                        codigoPostal = codigoPostal.trim(),
                        numeroPisos = numeroPisos.toIntOrNull() ?: 0,
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
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Distrito") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = direccion,
            onValueChange = { direccion = it },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Direccion") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = numeroDireccion,
            onValueChange = { numeroDireccion = it },
            shape = RoundedCornerShape(12.dp),
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
            shape = RoundedCornerShape(12.dp),
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
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Número de Pisos") },
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

// Pantalla principal con estado 100% desacoplado
@Composable
fun InmuebleScreen(
    viewModel: InmuebleViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.obtenerTodos()
    }

    when {
        uiState.mostrandoFormularioRegistro -> {
            RegistrarInmuebleScreen(
                onGuardar = { nuevoInmueble ->
                    viewModel.registrar(nuevoInmueble)
                },
                onCancelar = {
                    viewModel.mostrarFormularioRegistro(false)
                }
            )

            uiState.inmuebleInactivoDetectado?.let { inactivo ->
                AlertDialog(
                    onDismissRequest = { viewModel.cancelarReactivacion() },
                    title = { Text("Inmueble encontrado en el historial") },
                    text = {
                        Text("Este inmueble fue dado de baja anteriormente.\n\n¿Deseas reactivarlo y actualizar sus datos?")
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.reactivarInmueble(inactivo) }) {
                            Text("Reactivar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.cancelarReactivacion() }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            LaunchedEffect(uiState.successMessage) {
                if (uiState.successMessage != null) {
                    viewModel.limpiarMensajes()
                }
            }
        }

        uiState.inmuebleSeleccionado != null -> {
            EditarInmuebleScreen(
                inmueble = uiState.inmuebleSeleccionado!!,
                onGuardar = { actualizado ->
                    viewModel.actualizar(actualizado)
                },
                onCancelar = {
                    viewModel.seleccionarInmuebleParaEditar(null)
                }
            )
        }

        else -> {
            // Manejo del AlertDialog de Dar de Baja leyendo desde el estado
            if (uiState.inmuebleDarDeBaja != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.seleccionarInmuebleParaBaja(null) },
                    title = { Text("Dar de baja") },
                    text = { Text("¿Deseas dar de baja el inmueble seleccionado?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.darDeBaja(uiState.inmuebleDarDeBaja!!.idInmueble)
                            }
                        ) {
                            Text("Sí", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.seleccionarInmuebleParaBaja(null) }) {
                            Text("No")
                        }
                    }
                )
            }

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                // Barra superior
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
                            text = "Inmuebles",
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

                // Filtro visualmente alineado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.actualizarTipoBusqueda("Distrito") },
                        colors = if (uiState.tipoBusqueda == "Distrito") ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text("Distrito", color = if (uiState.tipoBusqueda == "Distrito") Color.White else MaterialTheme.colorScheme.primary)
                    }
                    Button(
                        onClick = { viewModel.actualizarTipoBusqueda("Código Postal") },
                        colors = if (uiState.tipoBusqueda == "Código Postal") ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text("Código Postal", color = if (uiState.tipoBusqueda == "Código Postal") Color.White else MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Barra de Búsqueda Moderna
                OutlinedTextField(
                    value = uiState.textoBusqueda,
                    onValueChange = { viewModel.actualizarTextoBusqueda(it) },
                    placeholder = { Text("Buscar por ${uiState.tipoBusqueda}...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = Color.Gray
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.LightGray
                    ),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (uiState.textoBusqueda.isNotBlank()) {
                                if (uiState.tipoBusqueda == "Distrito") {
                                    viewModel.obtenerPorDistrito(uiState.textoBusqueda)
                                } else {
                                    viewModel.obtenerPorCodigoPostal(uiState.textoBusqueda)
                                }
                            }
                        }
                    ) {
                        Text("Buscar")
                    }

                    Button(
                        onClick = {
                            viewModel.actualizarTextoBusqueda("")
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
                            InmuebleCard(
                                inmueble = inmueble,
                                onModificar = { viewModel.seleccionarInmuebleParaEditar(it) },
                                onDarDeBaja = { viewModel.seleccionarInmuebleParaBaja(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// DISEÑO DE LA TARJETA DEL INMUEBLE
@Composable
fun InmuebleCard(
    inmueble: Inmueble,
    onModificar: (Inmueble) -> Unit,
    onDarDeBaja: (Inmueble) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagen o Ícono
            if (!inmueble.fotografia.isNullOrBlank()) {
                val bytes = remember(inmueble.fotografia) { Base64.decode(inmueble.fotografia, Base64.DEFAULT) }
                AsyncImage(
                    model = bytes, contentDescription = null,
                    modifier = Modifier.size(70.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(70.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Información del inmueble (AHORA USA TYPE.KT)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = inmueble.distrito,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium // Toma el tamaño y grosor del Theme
                )
                Text(
                    text = "${inmueble.direccion} ${inmueble.numeroDireccion}",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "C.P: ${inmueble.codigoPostal}",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (inmueble.numeroPisos > 0) "# de pisos: ${inmueble.numeroPisos}" else "# de pisos: Sin registrar",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Botones
            Row {
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