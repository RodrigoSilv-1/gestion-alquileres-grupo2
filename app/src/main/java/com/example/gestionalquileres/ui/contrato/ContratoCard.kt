package com.example.gestionalquileres.ui.contrato

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gestionalquileres.domain.comoMonto
import com.example.gestionalquileres.domain.model.Contrato

@Composable
fun ContratoCard(
    contrato: Contrato,
    onFinalizar: (Contrato) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Inquilino: ${contrato.nombreInquilino}", fontWeight = FontWeight.Bold)
            Text(text = "DNI: ${contrato.dniInquilino}")
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Inmueble: ${contrato.direccionInmueble} (${contrato.distritoInmueble})")
            Text(text = "Unidad: ${contrato.nombreUnidad}")
            Text(text = "Renta Acordada: ${contrato.precioRentaAcordado.comoMonto()}")
            if (contrato.ingresoMensualInquilino > 0.0) {
                Text(text = "Ingreso del inquilino: ${contrato.ingresoMensualInquilino.comoMonto()}")
            }
            Text(text = "Vigencia: Desde ${contrato.fechaInicio} hasta ${contrato.fechaFin}")

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onFinalizar(contrato) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Finalizar Contrato")
            }
        }
    }
}