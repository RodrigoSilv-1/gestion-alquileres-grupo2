package com.example.gestionalquileres.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gestionalquileres.R
import com.example.gestionalquileres.domain.model.AppUser
import com.example.gestionalquileres.domain.model.UserRole
import com.example.gestionalquileres.ui.contrato.ContratoScreen
import com.example.gestionalquileres.ui.contrato.ContratoViewModel
import com.example.gestionalquileres.ui.inmuebles.InmuebleScreen
import com.example.gestionalquileres.ui.inmuebles.InmuebleViewModel
import com.example.gestionalquileres.ui.inquilinos.InquilinoScreen
import com.example.gestionalquileres.ui.inquilinos.InquilinoViewModel
import com.example.gestionalquileres.ui.recibos.ReciboScreen
import com.example.gestionalquileres.ui.recibos.ReciboViewModel

@Composable
fun HomeScreen(
    user: AppUser,
    onLogout: () -> Unit,
    inquilinoViewModel: InquilinoViewModel,
    inmuebleViewModel: InmuebleViewModel,
    contratoViewModel: ContratoViewModel,
    reciboViewModel: ReciboViewModel // <-- NUEVO PARÁMETRO
) {
    var mostrarInquilinos by rememberSaveable { mutableStateOf(false) }
    var mostrarInmuebles by rememberSaveable { mutableStateOf(false) }
    var mostrarContratos by rememberSaveable { mutableStateOf(false) }
    var mostrarRecibos by rememberSaveable { mutableStateOf(false) } // <-- NUEVO ESTADO DE NAVEGACIÓN

    if (mostrarInmuebles) {
        InmuebleScreen(
            viewModel = inmuebleViewModel,
            onVolver = { mostrarInmuebles = false }
        )
    } else if (mostrarInquilinos) {
        InquilinoScreen(
            viewModel = inquilinoViewModel,
            onVolver = { mostrarInquilinos = false }
        )
    } else if (mostrarContratos) {
        ContratoScreen(
            viewModel = contratoViewModel,
            onVolver = { mostrarContratos = false }
        )
    } else if (mostrarRecibos) { // <-- NUEVO BLOQUE DE NAVEGACIÓN
        ReciboScreen(
            viewModel = reciboViewModel,
            onVolver = { mostrarRecibos = false }
        )
    } else {
        val roleText = if (user.role == UserRole.ADMIN.name) "Administrador" else "Secretario"

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = R.drawable.edificio2),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.73f
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // CAJA CON INFORMACIÓN DEL USUARIO
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.90f),
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Sesión iniciada",
                            color = Color(0xFF222222),
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = user.email,
                            color = Color(0xFF333333),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Rol: $roleText",
                            color = Color(0xFF1565C0),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

// CAJA CON EL NOMBRE DE LOS MÓDULOS
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.90f),
                    shadowElevation = 3.dp
                ) {
                    Text(
                        text = if (user.role == UserRole.ADMIN.name) {
                            "Módulos del Administrador"
                        } else {
                            "Módulos del Secretario"
                        },
                        modifier = Modifier.padding(
                            horizontal = 20.dp,
                            vertical = 12.dp
                        ),
                        color = Color(0xFF222222),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (user.role == UserRole.ADMIN.name) {
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
                    Button(
                        onClick = {
                            mostrarRecibos = true
                        }
                    ) {
                        Text("Gestión de Recibos")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

// BOTÓN CERRAR SESIÓN CON FONDO
                Button(
                    onClick = onLogout,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.90f),
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = "Cerrar sesión",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
