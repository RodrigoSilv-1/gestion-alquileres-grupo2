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

@Composable
fun HomeScreen(
    user: AppUser,
    onLogout: () -> Unit,
    onIrInquilinos: () -> Unit,
    onIrInmuebles: () -> Unit,
    onIrContratos: () -> Unit,
    onIrRecibos: () -> Unit,
    onIrDashboard: () -> Unit
) {
    val esAdmin = user.role == UserRole.ADMIN.name
    val roleText = if (esAdmin) "Administrador" else "Secretario"

    Box(modifier = Modifier.fillMaxSize()) {
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
                    text = if (esAdmin) "Módulos del Administrador" else "Módulos del Secretario",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    color = Color(0xFF222222),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (esAdmin) {
                Button(onClick = onIrInquilinos) { Text("Gestión de Inquilinos") }
                Button(onClick = onIrInmuebles) { Text("Gestión de Inmuebles") }
                Button(onClick = onIrContratos) { Text("Gestión de Contratos") }
            } else {
                Button(onClick = onIrRecibos) { Text("Gestión de Recibos") }
                Button(onClick = onIrDashboard) { Text("Dashboard") }
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
                Text(text = "Cerrar sesión", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}