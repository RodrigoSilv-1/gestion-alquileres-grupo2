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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        null
    }
}

// Pantalla para registrar un nuevo inquilino
@Composable
fun RegistrarInquilinoScreen(
    onGuardar: (Inquilino) -> Unit,
    onCancelar: () -> Unit
) {
    // Obtiene el contexto actual de Android (necesario para acceder a recursos y funciones del sistema como la galería).
    val context = LocalContext.current

// usamos remenber y mutableStateOf para crear un objeto tipo InquilinoFormState
    //para avisar a la ui los cambios que se hagan
    var formState by remember { mutableStateOf(InquilinoFormState()) }

    // Estado booleano para controlar si el menú desplegable (Dropdown) del sexo está abierto (true) o cerrado (false).
    var sexoExpandido by remember { mutableStateOf(false) }

    // Registra un lanzador para abrir el selector de fotos nativo de Android (Photo Picker).
    val selectorFotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        // Cuando el usuario selecciona una imagen, el sistema nos devuelve su 'uri'
        if (uri != null) {
            // Comprimimos la imagen seleccionada y la convertimos a formato Base64 usando nuestra función auxiliar
            val fotoComprimida = uriABase64(context, uri)

            // Actualizamos el estado del formulario copiando los datos anteriores y agregando la foto en Base64
            formState = formState.copy(fotografiaBase64 = fotoComprimida)
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
            value = formState.nombre,
            onValueChange = { formState = formState.copy(nombre = it) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nombre") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = formState.dni,
            onValueChange = { formState = formState.copy(dni = it) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            label = { Text("DNI") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = formState.telefono,
            onValueChange = { formState = formState.copy(telefono = it) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Teléfono") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = formState.edad,
            onValueChange = { formState = formState.copy(edad = it) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Edad") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = formState.sueldo,
            onValueChange = { formState = formState.copy(sueldo = it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Sueldo Mensual ($)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Menú desplegable para el sexo
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = formState.sexo,
                onValueChange = {},
                readOnly = true,
                shape = RoundedCornerShape(12.dp),
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
                        formState = formState.copy(sexo = "Masculino")
                        sexoExpandido = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Femenino") },
                    onClick = {
                        formState = formState.copy(sexo = "Femenino")
                        sexoExpandido = false
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Fotografía", style = MaterialTheme.typography.titleMedium)

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
                // Verificamos si hay una foto guardada en el estado del formulario (que no sea nula ni vacía)
                if (!formState.fotografiaBase64.isNullOrBlank()) {

                    // Decodificamos el texto Base64 a un arreglo de bytes para que pueda ser leído por la librería de imágenes
                    val bytes = Base64.decode(formState.fotografiaBase64, Base64.DEFAULT)

                    // Componente de Coil que dibuja la imagen usando los bytes decodificados, ocupando todo el espacio de la caja
                    AsyncImage(
                        model = bytes,
                        contentDescription = "Foto inquilino",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Si NO hay foto, mostramos un texto de aviso dentro de la caja de previsualización
                    Text(
                        text = "Sin foto",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } // Cierre del contenedor visual de la foto

            // Columna que organiza los botones de acción al lado o debajo de la foto
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                // Botón principal para abrir la galería del celular
                Button(
                    onClick = {
                        // Lanzamos el selector nativo de Android filtrando únicamente por archivos de imagen
                        selectorFotoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    // Texto dinámico: si no hay foto dice "Elegir foto", si ya hay una dice "Cambiar foto"
                    Text(if (formState.fotografiaBase64 == null) "Elegir foto" else "Cambiar foto")
                }

                // Botón secundario condicional: solo aparece si ya se había seleccionado una foto
                if (formState.fotografiaBase64 != null) {
                    TextButton(onClick = {
                        // Al hacer clic, borramos la foto del estado asignándole un valor nulo
                        formState = formState.copy(fotografiaBase64 = null)
                    }) {
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
                    val nuevoInquilino = Inquilino(
                        nombre = formState.nombre.trim(),
                        dni = formState.dni.trim(),
                        telefono = formState.telefono.trim(),
                        edad = formState.edad.toIntOrNull() ?: 0,
                        sexo = formState.sexo.trim(),
                        fotografia = formState.fotografiaBase64,
                        sueldo = formState.sueldo.toDoubleOrNull() ?: 0.0
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
// Pantalla para actualizar datos de un inquilino optimizada con InquilinoFormState
@Composable
fun EditarInquilinoScreen(
    inquilino: Inquilino,
    onGuardar: (Inquilino) -> Unit,
    onCancelar: () -> Unit
) {
    // Obtiene el contexto actual de Android necesario para acceder a recursos y funciones del sistema
    val context = LocalContext.current

// Inicializamos el estado del formulario PRECARGANDO los datos del inquilino existente que se va a editar
    var formState by remember {
        mutableStateOf(
            InquilinoFormState(
                nombre = inquilino.nombre,
                dni = inquilino.dni,
                // Si la edad es mayor a 0, la convertimos a texto; si no, dejamos el campo vacío
                edad = if (inquilino.edad > 0) inquilino.edad.toString() else "",
                sexo = inquilino.sexo,
                telefono = inquilino.telefono,
                // Si el sueldo es mayor a 0, lo convertimos a texto; si no, vacío
                sueldo = if (inquilino.sueldo > 0.0) inquilino.sueldo.toString() else "",
                // Precargamos la fotografía en Base64 que el inquilino ya tenía guardada en la base de datos
                fotografiaBase64 = inquilino.fotografia
            )
        )
    }
    //uri: direccion donde se encuentra el archivo o foto en el dispositivo
// Estado booleano para controlar si el menú desplegable (Dropdown) del sexo está abierto o cerrado
    var sexoExpandido by remember { mutableStateOf(false) }

// En este bloque, consiste en que se va seleccionar una nueva foto accediendo a la galeria del sistema
    val selectorFotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        // Si el usuario selecciona una imagen de su galería
        if (uri != null) {
            // Comprimimos y convertimos la nueva imagen a formato Base64 usando nuestra función auxiliar
            val fotoComprimida = uriABase64(context, uri)

            // Actualizamos el estado del formulario con la nueva foto seleccionada
            formState = formState.copy(fotografiaBase64 = fotoComprimida)
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
            value = formState.nombre,
            onValueChange = { formState = formState.copy(nombre = it) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nombre") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = formState.dni,
            onValueChange = { formState = formState.copy(dni = it) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            label = { Text("DNI") }
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = formState.telefono,
            onValueChange = { formState = formState.copy(telefono = it) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Teléfono") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = formState.edad,
            onValueChange = { formState = formState.copy(edad = it) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Edad") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = formState.sueldo,
            onValueChange = { formState = formState.copy(sueldo = it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Sueldo Mensual ($)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Selector desplegable para actualizar sexo
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = formState.sexo,
                onValueChange = {},
                readOnly = true,
                shape = RoundedCornerShape(12.dp),
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
                        formState = formState.copy(sexo = "Masculino")
                        sexoExpandido = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Femenino") },
                    onClick = {
                        formState = formState.copy(sexo = "Femenino")
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
                if (!formState.fotografiaBase64.isNullOrBlank()) {
                    val bytes = Base64.decode(formState.fotografiaBase64, Base64.DEFAULT)
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
                    Text(if (formState.fotografiaBase64 == null) "Elegir foto" else "Cambiar foto")
                }

                if (formState.fotografiaBase64 != null) {
                    TextButton(onClick = { formState = formState.copy(fotografiaBase64 = null) }) {
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
                        nombre = formState.nombre.trim(),
                        dni = formState.dni.trim(),
                        telefono = formState.telefono.trim(),
                        edad = formState.edad.toIntOrNull() ?: inquilino.edad,
                        sexo = formState.sexo.trim(),
                        fotografia = formState.fotografiaBase64,
                        sueldo = formState.sueldo.toDoubleOrNull() ?: 0.0
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

@Composable
fun InquilinoScreen(
    viewModel: InquilinoViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    //efecto que obtiene la lista de inquilinos mediante vm
    LaunchedEffect(Unit) {
        viewModel.obtenerTodos()
    }

    when {
        uiState.mostrandoFormularioRegistro -> {
            RegistrarInquilinoScreen(
                onGuardar = { nuevoInquilino ->
                    viewModel.registrar(nuevoInquilino)
                },
                onCancelar = {
                    viewModel.mostrarFormularioRegistro(false)
                }
            )

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
                            onClick = { viewModel.reactivarInquilino(inactivo) }
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

            LaunchedEffect(uiState.successMessage) {
                if (uiState.successMessage != null) {
                    viewModel.limpiarMensajes()
                }
            }
        }

        uiState.inquilinoSeleccionado != null -> {
            EditarInquilinoScreen(
                inquilino = uiState.inquilinoSeleccionado!!,
                onGuardar = { actualizado ->
                    viewModel.actualizar(actualizado)
                },
                onCancelar = {
                    viewModel.seleccionarInquilinoParaEditar(null)
                }
            )
        }

        else -> {
            if (uiState.inquilinoDarDeBaja != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.seleccionarInquilinoParaBaja(null) },
                    title = { Text("Dar de baja") },
                    text = { Text("¿Deseas dar de baja a ${uiState.inquilinoDarDeBaja!!.nombre}?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.darDeBaja(uiState.inquilinoDarDeBaja!!.idInquilino)
                            }
                        ) {
                            Text("Sí", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.seleccionarInquilinoParaBaja(null) }) {
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
                            viewModel.mostrarFormularioRegistro(true)
                        }
                    ) {
                        Text("Nuevo")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.actualizarTipoBusqueda("Nombre") },
                        colors = if (uiState.tipoBusqueda == "Nombre") ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text("Nombre", color = if (uiState.tipoBusqueda == "Nombre") Color.White else MaterialTheme.colorScheme.primary)
                    }
                    Button(
                        onClick = { viewModel.actualizarTipoBusqueda("DNI") },
                        colors = if (uiState.tipoBusqueda == "DNI") ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text("DNI", color = if (uiState.tipoBusqueda == "DNI") Color.White else MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (uiState.textoBusqueda.isNotBlank()) {
                                if (uiState.tipoBusqueda == "Nombre") {
                                    viewModel.obtenerPorNombre(uiState.textoBusqueda)
                                } else {
                                    viewModel.obtenerPorDni(uiState.textoBusqueda)
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

                if (uiState.isLoading) {
                    Text("Cargando lista de inquilinos...")
                } else if (uiState.inquilinos.isEmpty()) {
                    Text("No hay inquilinos registrados.")
                } else {
                    // Contenedor optimizado para listas (equivalente al RecyclerView antiguo);
                    // solo renderiza en pantalla los elementos visibles para ahorrar memoria.
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp) // Deja un espacio de 8 píxeles entre cada tarjeta de la lista
                    ) {
                        // Recorre la lista de inquilinos que viene del estado de la UI (uiState)
                        items(uiState.inquilinos) { inquilino ->

                            // Componente visual personalizado que dibuja los datos de cada inquilino en pantalla
                            InquilinoCard(
                                inquilino = inquilino,

                                // Callback: Si el usuario presiona el botón "Modificar", avisa al ViewModel enviándole el inquilino (it)
                                onModificar = { viewModel.seleccionarInquilinoParaEditar(it) },

                                // Callback: Si el usuario presiona "Dar de baja", avisa al ViewModel enviándole el inquilino (it)
                                onDarDeBaja = { viewModel.seleccionarInquilinoParaBaja(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// NUEVO DISEÑO PARA LA TARJETA DEL INQUILINO
@Composable
fun InquilinoCard(
    inquilino: Inquilino,
    onModificar: (Inquilino) -> Unit,
    onDarDeBaja: (Inquilino) -> Unit
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
            // Verificamos si el inquilino tiene una foto registrada (que no sea nula ni esté vacía)
            if (!inquilino.fotografia.isNullOrBlank()) {

                // Decodificamos el texto Base64 a un arreglo de bytes.
                // Usamos 'remember' para que la decodificación solo se haga cuando la foto cambie, optimizando el rendimiento.
                val bytes = remember(inquilino.fotografia) { Base64.decode(inquilino.fotografia, Base64.DEFAULT) }

                // Componente de Coil para cargar imágenes; en este caso recibe los bytes decodificados,
                // los recorta en forma circular de 50x50 dp y ajusta la escala (Crop).
                AsyncImage(
                    model = bytes,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Si NO tiene foto, mostramos un avatar por defecto con sus iniciales
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), // Fondo circular semitransparente
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        // Extrae la primera letra del nombre, la pasa a mayúsculas;
                        // si el nombre está vacío, usa el operador Elvis '?:' para poner un signo "?"
                        text = inquilino.nombre.firstOrNull()?.uppercase() ?: "?",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Información del inquilino (AHORA USA TYPE.KT)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = inquilino.nombre,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium // Toma el tamaño de 18.sp semi-bold de Type.kt
                )
                Text(
                    text = "DNI: ${inquilino.dni} • Edad: ${inquilino.edad}",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium // Toma el tamaño de 14.sp normal
                )
                Text(
                    text = if (inquilino.telefono.isNotBlank()) "Tel: ${inquilino.telefono}" else "Tel: Sin registrar",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall // Toma el tamaño de 12.sp
                )
            }

            // Botones
            Row {
                IconButton(onClick = { onModificar(inquilino) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Modificar", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { onDarDeBaja(inquilino) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Dar de baja", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}