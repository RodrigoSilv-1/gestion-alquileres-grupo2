package com.example.gestionalquileres.ui.inquilinos

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gestionalquileres.domain.model.Inquilino
import com.example.gestionalquileres.ui.common.CampoTexto
import com.example.gestionalquileres.ui.common.FotoPicker

@Composable
fun RegistrarInquilinoScreen(
    onGuardar: (Inquilino) -> Unit,
    onCancelar: () -> Unit,
    guardando: Boolean = false
) {
    val form = remember { InquilinoFormState() }
    InquilinoFormulario(
        textoGuardar = "Guardar",
        form = form,
        guardando = guardando,
        onGuardar = { onGuardar(form.toInquilino()) },
        onCancelar = onCancelar
    )
}

@Composable
fun EditarInquilinoScreen(
    inquilino: Inquilino,
    onGuardar: (Inquilino) -> Unit,
    onCancelar: () -> Unit,
    guardando: Boolean = false
) {
    val form = remember(inquilino.idInquilino) { InquilinoFormState(inquilino) }
    InquilinoFormulario(
        textoGuardar = "Guardar cambios",
        form = form,
        guardando = guardando,
        onGuardar = { onGuardar(form.toInquilino(base = inquilino)) },
        onCancelar = onCancelar
    )
}

@Composable
private fun InquilinoFormulario(
    textoGuardar: String,
    form: InquilinoFormState,
    guardando: Boolean,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CampoTexto(form.nombre, { form.nombre = it }, "Nombre")
        CampoTexto(form.dni, { form.dni = it }, "DNI")
        CampoTexto(form.telefono, { form.telefono = it }, "Teléfono", KeyboardType.Phone)
        CampoTexto(form.edad, { form.edad = it }, "Edad", KeyboardType.Number, soloDigitos = true)
        CampoTexto(form.sueldo, { form.sueldo = it }, "Sueldo Mensual ($)", KeyboardType.Number)
        SelectorSexo(valor = form.sexo, onSeleccionar = { form.sexo = it })

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
                enabled = form.esValido && !guardando,
                modifier = Modifier.weight(1f)
            ) {
                if (guardando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(textoGuardar)
            }
        }
    }
}

private val OPCIONES_SEXO = listOf("Masculino", "Femenino")

@Composable
private fun SelectorSexo(valor: String, onSeleccionar: (String) -> Unit) {
    var expandido by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = valor,
            onValueChange = {},
            readOnly = true,
            label = { Text("Sexo") },
            trailingIcon = { Text(if (expandido) "▲" else "▼") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        // Capa transparente: hace que todo el campo abra el menú
        Box(Modifier.matchParentSize().clickable { expandido = true })

        DropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            OPCIONES_SEXO.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion) },
                    onClick = {
                        onSeleccionar(opcion)
                        expandido = false
                    }
                )
            }
        }
    }
}