package com.example.gestionalquileres

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.gestionalquileres.domain.model.AppUser
import com.example.gestionalquileres.domain.model.UserRole
import com.example.gestionalquileres.ui.auth.AuthUiState
import com.example.gestionalquileres.ui.auth.AuthViewModel
import com.example.gestionalquileres.ui.inmuebles.InmuebleScreen
import com.example.gestionalquileres.ui.inmuebles.InmuebleViewModel
import com.example.gestionalquileres.ui.inquilinos.InquilinoViewModel
import com.example.gestionalquileres.ui.inquilinos.InquilinoScreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.example.gestionalquileres.ui.contrato.ContratoScreen
import com.example.gestionalquileres.ui.contrato.ContratoViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val inquilinoViewModel: InquilinoViewModel by viewModels()
    private val inmuebleViewModel: InmuebleViewModel by viewModels()

    private val contratoViewModel: ContratoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val uiState by authViewModel.uiState.collectAsState()
                var showRegister by rememberSaveable { mutableStateOf(false) }

                when {
                    uiState.isLoading -> LoadingScreen()

                    uiState.currentUser != null -> {
                        HomeScreen(
                            user = uiState.currentUser!!,
                            onLogout = authViewModel::logout,
                            inquilinoViewModel = inquilinoViewModel,
                            inmuebleViewModel = inmuebleViewModel,
                            contratoViewModel = contratoViewModel
                        )
                    }

                    showRegister -> RegisterScreen(
                        uiState = uiState,
                        onRegister = authViewModel::register,
                        onGoToLogin = { showRegister = false },
                        onClearError = authViewModel::clearError,
                        onGoogleSignIn = authViewModel::signInWithGoogle,
                        onGoogleError = authViewModel::showError

                    )

                    else -> LoginScreen(
                        uiState = uiState,
                        onLogin = authViewModel::login,
                        onGoToRegister = { showRegister = true },
                        onClearError = authViewModel::clearError,
                        onGoogleSignIn = authViewModel::signInWithGoogle,
                        onGoogleError = authViewModel::showError
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text("Cargando...")
    }
}

@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onLogin: (String, String) -> Unit,
    onGoToRegister: () -> Unit,
    onClearError: () -> Unit,
    onGoogleSignIn: (String, UserRole) -> Unit,
    onGoogleError: (String) -> Unit

) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            delay(3000)
            onClearError()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // FOTO DE FONDO
        Image(
            painter = painterResource(id = R.drawable.edificios),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // DEGRADADO
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(alpha = 0.35f)
                )
        )

        // CONTENIDO
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(50.dp))



            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Gestión de Alquileres",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Iniciar sesión",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo electrónico") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    unfocusedBorderColor = Color(0xFF555555),
                    focusedBorderColor = Color(0xFF1565C0),
                    unfocusedLabelColor = Color(0xFF555555),
                    focusedLabelColor = Color(0xFF1565C0)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,

                    unfocusedBorderColor = Color(0xFF555555),
                    focusedBorderColor = Color(0xFF1565C0),

                    unfocusedLabelColor = Color(0xFF555555),
                    focusedLabelColor = Color(0xFF1565C0)
                )
            )

            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = uiState.errorMessage,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onLogin(email, password) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1565C0),
                    contentColor = Color.White
                )
            ) {
                Text("Iniciar sesión")
            }
            Spacer(modifier = Modifier.height(12.dp))

            GoogleSignInButton(
                role = UserRole.ADMIN,
                onGoogleToken = onGoogleSignIn,
                onGoogleError = onGoogleError
            )
            TextButton(
                onClick = onGoToRegister
            ) {
                Text(
                    text = "¿No tienes cuenta? Regístrate",
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun RegisterScreen(
    uiState: AuthUiState,
    onRegister: (String, String, String, UserRole) -> Unit,
    onGoToLogin: () -> Unit,
    onClearError: () -> Unit,
    onGoogleSignIn: (String, UserRole) -> Unit,
    onGoogleError: (String) -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var selectedRole by rememberSaveable {
        mutableStateOf(UserRole.SECRETARIO.name)
    }
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            delay(3000)
            onClearError()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // IMAGEN DE FONDO
        Image(
            painter = painterResource(id = R.drawable.edificios),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // CAPA OSCURA
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(alpha = 0.35f)
                )
        )

        // CONTENIDO
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(50.dp))

            Text(
                text = "Crear cuenta",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // CORREO
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = {
                    Text("Correo electrónico")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    unfocusedBorderColor = Color(0xFF555555),
                    focusedBorderColor = Color(0xFF1565C0),
                    unfocusedLabelColor = Color(0xFF555555),
                    focusedLabelColor = Color(0xFF1565C0)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // CONTRASEÑA
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = {
                    Text("Contraseña mínimo 6 caracteres")
                },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    unfocusedBorderColor = Color(0xFF555555),
                    focusedBorderColor = Color(0xFF1565C0),
                    unfocusedLabelColor = Color(0xFF555555),
                    focusedLabelColor = Color(0xFF1565C0)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // CONFIRMAR CONTRASEÑA
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = {
                    Text("Confirmar contraseña")
                },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    unfocusedBorderColor = Color(0xFF555555),
                    focusedBorderColor = Color(0xFF1565C0),
                    unfocusedLabelColor = Color(0xFF555555),
                    focusedLabelColor = Color(0xFF1565C0)
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Selecciona un rol")

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedRole == UserRole.SECRETARIO.name,
                    onClick = { selectedRole = UserRole.SECRETARIO.name }
                )
                Text("Secretario")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedRole == UserRole.ADMIN.name,
                    onClick = { selectedRole = UserRole.ADMIN.name }
                )
                Text("Administrador")
            }
            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = uiState.errorMessage,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // BOTÓN REGISTRAR
            Button(
                onClick = {
                    onRegister(
                        email,
                        password,
                        confirmPassword,
                        UserRole.valueOf(selectedRole)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1565C0),
                    contentColor = Color.White
                )
            ) {
                Text("Registrarme")
            }

            Spacer(modifier = Modifier.height(12.dp))

            GoogleSignInButton(
                role = UserRole.valueOf(selectedRole),
                onGoogleToken = onGoogleSignIn,
                onGoogleError = onGoogleError
            )

            // VOLVER AL LOGIN
            TextButton(
                onClick = onGoToLogin
            ) {
                Text(
                    text = "Ya tengo una cuenta",
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    user: AppUser,
    onLogout: () -> Unit,
    inquilinoViewModel: InquilinoViewModel,
    inmuebleViewModel: InmuebleViewModel,
    contratoViewModel: ContratoViewModel
) {
    var mostrarInquilinos by rememberSaveable {
        mutableStateOf(false)
    }

    var mostrarInmuebles by rememberSaveable() {
        mutableStateOf(false)
    }

    var mostrarContratos by rememberSaveable { mutableStateOf(false) }

    if(mostrarInmuebles) {
        InmuebleScreen(
            viewModel = inmuebleViewModel,
            onVolver = {
                mostrarInmuebles = false
            }
        )
    } else if (mostrarInquilinos) {
        InquilinoScreen(
            viewModel = inquilinoViewModel,
            onVolver = {
                mostrarInquilinos = false
            }
        )
    } else if (mostrarContratos) { // <-- NUEVA PANTALLA
        ContratoScreen(
            viewModel = contratoViewModel,
            onVolver = { mostrarContratos = false }
        )
    } else {
        val roleText = if (user.role == UserRole.ADMIN.name) {
            "Administrador"
        } else {
            "Secretario"
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Sesión iniciada",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(user.email)

            Text("Rol: $roleText")

            Spacer(modifier = Modifier.height(24.dp))

            if (user.role == UserRole.ADMIN.name) {
                Text("Módulos del Administrador")

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        mostrarInquilinos = true
                    }
                ) {
                    Text("Gestión de Inquilinos")
                }

                Button(
                    onClick = {
                        mostrarInmuebles = true
                    }
                ) {
                    Text("Gestión de Inmuebles")
                }

                Button(
                    onClick = {
                        mostrarContratos = true
                    }
                ) {
                    Text("Gestión de Contratos")
                }
            } else {
                Text("Aquí irán los módulos de recibos y cobros.")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onLogout
            ) {
                Text("Cerrar sesión")
            }
        }
    }
}
@Composable
fun GoogleSignInButton(
    role: UserRole,
    onGoogleToken: (String, UserRole) -> Unit,
    onGoogleError: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val webClientId = stringResource(R.string.google_web_client_id)
    Button(
        onClick = {
            coroutineScope.launch {
                try {
                    val credentialManager = CredentialManager.create(context)

                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(webClientId)
                        .setAutoSelectEnabled(false)
                        .build()

                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    val result = credentialManager.getCredential(
                        context,
                        request
                    )

                    val credential = result.credential

                    if (
                        credential is CustomCredential &&
                        credential.type ==
                        GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    ) {
                        val googleCredential =
                            GoogleIdTokenCredential.createFrom(credential.data)

                        onGoogleToken(
                            googleCredential.idToken,
                            role
                        )
                    } else {
                        onGoogleError(
                            "No se pudo obtener una cuenta de Google."
                        )
                    }
                } catch (error: Exception) {
                    onGoogleError(
                        "No se pudo iniciar sesión con Google."
                    )
                }
            }

        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1565C0),
            contentColor = Color.White
        )
    ) {
        Text("Continuar con Google")
    }
}