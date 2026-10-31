package com.example.gestionalquileres.ui.inmuebles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gestionalquileres.domain.model.Inmueble
import com.example.gestionalquileres.ui.common.FotoPicker

@Composable
fun RegistrarInmuebleScreen(
    onGuardar: (Inmueble) -> Unit,
    onCancelar: () -> Unit,
    guardando: Boolean = false
) {
    val form = remember { InmuebleFormState() }
    InmuebleFormulario(
        titulo = "Registrar Inmueble",
        textoGuardar = "Guardar",
        form = form,
        esNuevo = true,
        guardando = guardando,
        onGuardar = { onGuardar(form.toInmueble()) },
        onCancelar = onCancelar
    )
}

@Composable
fun EditarInmuebleScreen(
    inmueble: Inmueble,
    onGuardar: (Inmueble) -> Unit,
    onCancelar: () -> Unit,
    guardando: Boolean = false
) {
    val form = remember(inmueble.idInmueble) { InmuebleFormState(inmueble) }
    InmuebleFormulario(
        titulo = "Modificar Inmueble",
        textoGuardar = "Guardar cambios",
        form = form,
        esNuevo = false,
        guardando = guardando,
        onGuardar = { onGuardar(form.toInmueble(base = inmueble)) },
        onCancelar = onCancelar
    )
}

@Composable
private fun InmuebleFormulario(
    titulo: String,
    textoGuardar: String,
    form: InmuebleFormState,
    esNuevo: Boolean,
    guardando: Boolean,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(titulo, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))

        CampoTexto(form.distrito, { form.distrito = it }, "Distrito")
        CampoTexto(form.direccion, { form.direccion = it }, "Dirección")
        CampoTexto(form.numeroDireccion, { form.numeroDireccion = it }, "Número de Dirección", KeyboardType.Number)
        CampoTexto(form.codigoPostal, { form.codigoPostal = it }, "Código Postal", KeyboardType.Number)

        // Pisos y unidades solo se definen al registrar (generan las unidades alquilables)
        CampoTexto(form.numeroPisos, { form.numeroPisos = it }, "Número de Pisos", KeyboardType.Number, soloDigitos = true, enabled = esNuevo)
        CampoTexto(form.unidadesPorPiso, { form.unidadesPorPiso = it }, "Unidades / Departamentos por Piso", KeyboardType.Number, soloDigitos = true, enabled = esNuevo)

        Spacer(Modifier.height(8.dp))
        Text("Fotografía", style = MaterialTheme.typography.titleMedium)
        FotoPicker(fotografiaBase64 = form.fotografia, onFotoChange = { form.fotografia = it })

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancelar, enabled = !guardando, modifier = Modifier.weight(1f)) {
                Text("Cancelar")
            }
            Button(
                onClick = onGuardar,
                enabled = form.esValido(esNuevo) && !guardando,
                modifier = Modifier.weight(1f)
            ) {
                if (guardando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(textoGuardar)
            }
        }
    }
}

@Composable
private fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    teclado: KeyboardType = KeyboardType.Text,
    soloDigitos: Boolean = false,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { onCambio(if (soloDigitos) it.filter(Char::isDigit) else it) },
        label = { Text(etiqueta) },
        enabled = enabled,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        modifier = Modifier.fillMaxWidth()
    )
}