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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gestionalquileres.domain.SIMBOLO_MONEDA
import com.example.gestionalquileres.domain.limpiarMonto
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
        esNuevo = true,
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
        esNuevo = false,
        guardando = guardando,
        onGuardar = { onGuardar(form.toInquilino(base = inquilino)) },
        onCancelar = onCancelar
    )
}

@Composable
private fun InquilinoFormulario(
    textoGuardar: String,
    form: InquilinoFormState,
    esNuevo: Boolean,
    guardando: Boolean,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit
) {
    var abrirSexo by remember { mutableIntStateOf(0) }
    var abrirFoto by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CampoTexto(
            form.nombre, { form.nombre = it }, "Nombre completo",
            esError = form.errorNombre != null, ayuda = form.errorNombre
        )
        CampoTexto(
            form.dni, { form.dni = it.take(LARGO_DNI) }, "DNI",
            KeyboardType.Number, soloDigitos = true, enabled = esNuevo,
            esError = form.errorDni != null,
            ayuda = form.errorDni ?: if (!esNuevo) "El DNI no se puede modificar." else null
        )
        CampoTexto(
            form.telefono, { form.telefono = it.take(LARGO_TELEFONO) }, "Celular",
            KeyboardType.Phone, soloDigitos = true,
            esError = form.errorTelefono != null, ayuda = form.errorTelefono
        )
        CampoTexto(
            form.edad, { form.edad = it.take(2) }, "Edad",
            KeyboardType.Number, soloDigitos = true,
            esError = form.errorEdad != null, ayuda = form.errorEdad
        )
        CampoTexto(
            form.sueldo, { form.sueldo = limpiarMonto(it) }, "Sueldo Mensual ($SIMBOLO_MONEDA)",
            KeyboardType.Decimal,
            esError = form.errorSueldo != null,
            ayuda = form.errorSueldo ?: "Se usa para evaluar la solvencia (debe ser al menos el doble de la renta).",
            imeAction = ImeAction.Done,
            onDone = { abrirSexo++ }
        )
        SelectorSexo(
            valor = form.sexo,
            onSeleccionar = {
                form.sexo = it
                abrirFoto++            // al elegir sexo, pasa a elegir foto
            },
            abrirSolicitud = abrirSexo
        )

        Spacer(Modifier.height(8.dp))
        Text("Fotografía", style = MaterialTheme.typography.titleMedium)
        FotoPicker(
            fotografiaBase64 = form.fotografia,
            onFotoChange = { miniatura, completa ->
                form.fotografia = miniatura
                form.fotoCompleta = completa
            },
            enfocarSolicitud  = abrirFoto
        )

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

private val OPCIONES_SEXO = listOf("Masculino", "Femenino")

@Composable
private fun SelectorSexo(
    valor: String,
    onSeleccionar: (String) -> Unit,
    abrirSolicitud: Int = 0
) {
    var expandido by remember { mutableStateOf(false) }

    LaunchedEffect(abrirSolicitud) {
        if (abrirSolicitud > 0) expandido = true
    }

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