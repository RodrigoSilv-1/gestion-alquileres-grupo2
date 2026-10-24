package com.example.gestionalquileres.ui.common

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val FORMATO_VISIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy")

// El DatePicker trabaja con milisegundos en UTC (medianoche)
private fun LocalDate.aMillisUtc(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.aLocalDateUtc(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/**
 * Campo de solo lectura que abre un calendario al tocarlo.
 * [fecha] y el valor devuelto en [onFechaSeleccionada] usan el formato "yyyy-MM-dd".
 * Las fechas fuera de [fechaMinima]..[fechaMaxima] aparecen deshabilitadas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoFecha(
    label: String,
    fecha: String,
    onFechaSeleccionada: (String) -> Unit,
    fechaMinima: LocalDate,
    modifier: Modifier = Modifier,
    fechaMaxima: LocalDate? = null,
    habilitado: Boolean = true,
    textoAyuda: String? = null
) {
    var mostrarDialogo by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    // Un TextField readOnly consume el clic, así que se detecta el "soltar" del toque
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaccion ->
            if (interaccion is PressInteraction.Release) mostrarDialogo = true
        }
    }

    val fechaActual = runCatching { LocalDate.parse(fecha) }.getOrNull()

    OutlinedTextField(
        value = fechaActual?.format(FORMATO_VISIBLE).orEmpty(),
        onValueChange = {},
        readOnly = true,
        enabled = habilitado,
        label = { Text(label) },
        placeholder = { Text("dd/mm/aaaa") },
        trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Elegir fecha") },
        supportingText = if (textoAyuda != null) { { Text(textoAyuda) } } else null,
        singleLine = true,
        interactionSource = interactionSource,
        modifier = modifier.fillMaxWidth()
    )

    if (mostrarDialogo) {
        val fechasPermitidas = remember(fechaMinima, fechaMaxima) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val dia = utcTimeMillis.aLocalDateUtc()
                    return !dia.isBefore(fechaMinima) &&
                            (fechaMaxima == null || !dia.isAfter(fechaMaxima))
                }

                override fun isSelectableYear(year: Int): Boolean =
                    year >= fechaMinima.year && (fechaMaxima == null || year <= fechaMaxima.year)
            }
        }

        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = fechaActual?.aMillisUtc(),
            initialDisplayedMonthMillis = (fechaActual ?: fechaMinima).aMillisUtc(),
            selectableDates = fechasPermitidas
        )

        DatePickerDialog(
            onDismissRequest = { mostrarDialogo = false },
            confirmButton = {
                TextButton(
                    enabled = pickerState.selectedDateMillis != null,
                    onClick = {
                        pickerState.selectedDateMillis?.let {
                            onFechaSeleccionada(it.aLocalDateUtc().toString()) // yyyy-MM-dd
                        }
                        mostrarDialogo = false
                    }
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}