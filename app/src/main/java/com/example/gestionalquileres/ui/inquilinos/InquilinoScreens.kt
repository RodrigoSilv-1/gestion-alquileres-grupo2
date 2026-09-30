package com.example.gestionalquileres.ui.inquilinos

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.gestionalquileres.domain.model.Inquilino
import java.io.ByteArrayOutputStream

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
        null    }
}

// Pantalla para registrar un nuevo inquilino
@Composable
fun RegistrarInquilinoScreen(
    onGuardar: (Inquilino) -> Unit,
    onCancelar: () -> Unit
) {
    val context = LocalContext.current
    var nombre by remember { mutableStateOf("") }
    var dni by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }
    var sexo by remember { mutableStateOf("") }
    var fotografiaBase64 by remember { mutableStateOf<String?>(null) }
    var telefono by remember { mutableStateOf("") }

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
            text = "Registrar Inquilino",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nombre") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = dni,
            onValueChange = { dni = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("DNI") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = telefono,
            onValueChange = { telefono = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Teléfono") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = edad,
            onValueChange = { edad = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Edad") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        var sexoExpandido by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = sexo,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Sexo") },
                trailingIcon = { Text(if (sexoExpandido) "▲" else "▼") }
            )
            // Captura el clic sobre todo el campo para abrir el menu
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { sexoExpandido = true }
            )
            DropdownMenu(
                expanded = sexoExpandido,
                onDismissRequest = { sexoExpandido = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                DropdownMenuItem(
                    text = { Text("Masculino") },
                    onClick = {
                        sexo = "Masculino"
                        sexoExpandido = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Femenino") },
                    onClick = {
                        sexo = "Femenino"
                        sexoExpandido = false
                    }
                )
            }
        }

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
                        contentDescription = "Foto inquilino",
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

        // Botones de accion
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
                    val nuevoInquilino = Inquilino(
                        nombre = nombre.trim(),
                        dni = dni.trim(),
                        telefono = telefono.trim(),
                        edad = edad.toIntOrNull() ?: 0,
                        sexo = sexo.trim(),
                        fotografia = fotografiaBase64
                    )
                    onGuardar(nuevoInquilino)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Guardar")
            }
        }
    }
}

// Pantalla para actualizar datos de un inquilino
@Composable
fun EditarInquilinoScreen(
    inquilino: Inquilino,
    onGuardar: (Inquilino) -> Unit,
    onCancelar: () -> Unit
) {
    val context = LocalContext.current
    var nombre by remember { mutableStateOf(inquilino.nombre) }
    var dni by remember { mutableStateOf(inquilino.dni) }
    var edad by remember { mutableStateOf(inquilino.edad.toString()) }
    var sexo by remember { mutableStateOf(inquilino.sexo) }
    var fotografiaBase64 by remember { mutableStateOf(inquilino.fotografia) }
    var telefono by remember { mutableStateOf(inquilino.telefono) }

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
            text = "Modificar Inquilino",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nombre") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = dni,
            onValueChange = { dni = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("DNI") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = telefono,
            onValueChange = { telefono = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Teléfono") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = edad,
            onValueChange = { edad = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Edad") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Selector desplegable para actualizar sexo
        var sexoExpandido by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = sexo,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Sexo") },
                trailingIcon = { Text(if (sexoExpandido) "▲" else "▼") }
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { sexoExpandido = true }
            )
            DropdownMenu(
                expanded = sexoExpandido,
                onDismissRequest = { sexoExpandido = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                DropdownMenuItem(
                    text = { Text("Masculino") },
                    onClick = {
                        sexo = "Masculino"
                        sexoExpandido = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Femenino") },
                    onClick = {
                        sexo = "Femenino"
                        sexoExpandido = false
                    }
                )
            }
        }

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
                        contentDescription = "Foto inquilino",
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
                    val inquilinoActualizado = inquilino.copy(
                        nombre = nombre.trim(),
                        dni = dni.trim(),
                        telefono = telefono.trim(),
                        edad = edad.toIntOrNull() ?: inquilino.edad,
                        sexo = sexo.trim(),
                        fotografia = fotografiaBase64
                    )
                    onGuardar(inquilinoActualizado)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Guardar cambios")
            }
        }
    }
}

// Pantalla principal: listado, busquedas y control de navegacion
@Composable
fun InquilinoScreen(
    viewModel: InquilinoViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Carga la lista inicial desde la base de datos
    LaunchedEffect(Unit) {
        viewModel.obtenerTodos()
    }

    var inquilinoAEditar by remember { mutableStateOf<Inquilino?>(null) }
    var mostrandoFormularioRegistro by remember { mutableStateOf(false) }
    var tipoBusqueda by remember { mutableStateOf("Nombre") }
    var textoBusqueda by remember { mutableStateOf("") }

    when {
        mostrandoFormularioRegistro -> {
            RegistrarInquilinoScreen(
                onGuardar = { nuevoInquilino ->
                    viewModel.registrar(nuevoInquilino)
                },
                onCancelar = {
                    mostrandoFormularioRegistro = false
                }
            )

            // Alerta si el DNI ingresado ya existia pero estaba dado de baja
            uiState.inquilinoInactivoDetectado?.let { inactivo ->
                AlertDialog(
                    onDismissRequest = { viewModel.cancelarReactivacion() },
                    title = { Text("Inquilino encontrado en el historial") },
                    text = {
                        Text(
                            "El DNI ${inactivo.dni} pertenece a un inquilino dado de baja anteriormente.\n\n" +
                                    "¿Deseas reactivar su expediente y actualizar sus datos?"
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.reactivarInquilino(inactivo)
                                mostrandoFormularioRegistro = false
                            }
                        ) {
                            Text("Reactivar")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.cancelarReactivacion() }
                        ) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            // Cierra el formulario cuando la operacion se guarda correctamente
            LaunchedEffect(uiState.successMessage) {
                if (uiState.successMessage != null) {
                    mostrandoFormularioRegistro = false
                    viewModel.limpiarMensajes()
                }
            }
        }

        inquilinoAEditar != null -> {
            EditarInquilinoScreen(
                inquilino = inquilinoAEditar!!,
                onGuardar = { actualizado ->
                    viewModel.actualizar(actualizado)
                    inquilinoAEditar = null
                },
                onCancelar = {
                    inquilinoAEditar = null
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
                // Barra superior: volver y boton para nuevo registro
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
                            text = "Lista de Inquilinos",
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

                // Filtro para elegir criterio de busqueda
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { tipoBusqueda = "Nombre" }) {
                        Text("Nombre")
                    }
                    Button(onClick = { tipoBusqueda = "DNI" }) {
                        Text("DNI")
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
                                if (tipoBusqueda == "Nombre") {
                                    viewModel.obtenerPorNombre(textoBusqueda)
                                } else {
                                    viewModel.obtenerPorDni(textoBusqueda)
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

                // Listado de inquilinos activos
                if (uiState.isLoading) {
                    Text("Cargando lista de inquilinos...")
                } else if (uiState.inquilinos.isEmpty()) {
                    Text("No hay inquilinos registrados.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.inquilinos) { inquilino ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Muestra foto si tiene, o circulo con la inicial si no
                                    if (!inquilino.fotografia.isNullOrBlank()) {
                                        val bytes = remember(inquilino.fotografia) {
                                            Base64.decode(inquilino.fotografia, Base64.DEFAULT)
                                        }
                                        AsyncImage(
                                            model = bytes,
                                            contentDescription = "Foto de ${inquilino.nombre}",
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = inquilino.nombre.firstOrNull()?.uppercase() ?: "?",
                                                style = MaterialTheme.typography.titleLarge,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }

                                    // Datos principales y botones de gestion
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = inquilino.nombre,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "DNI: ${inquilino.dni} • Edad: ${inquilino.edad}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = if (inquilino.telefono.isNotBlank()) "Tel: ${inquilino.telefono}" else "Tel: Sin registrar",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(onClick = { inquilinoAEditar = inquilino }) {
                                                Text("Modificar")
                                            }

                                            var inquilinoDarDeBaja by remember {
                                                mutableStateOf<Inquilino?>(null)
                                            }

                                            OutlinedButton(onClick = { inquilinoDarDeBaja = inquilino }) {
                                                Text("Dar de baja")
                                            }

                                            // Confirmacion antes de aplicar la baja logica
                                            if (inquilinoDarDeBaja != null) {
                                                AlertDialog(
                                                    onDismissRequest = { inquilinoDarDeBaja = null },
                                                    title = { Text("Dar de baja") },
                                                    text = { Text("¿Deseas dar de baja a ${inquilinoDarDeBaja!!.nombre}?") },
                                                    confirmButton = {
                                                        TextButton(
                                                            onClick = {
                                                                viewModel.darDeBaja(inquilinoDarDeBaja!!.idInquilino)
                                                                inquilinoDarDeBaja = null
                                                            }
                                                        ) {
                                                            Text("Sí")
                                                        }
                                                    },
                                                    dismissButton = {
                                                        TextButton(onClick = { inquilinoDarDeBaja = null }) {
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