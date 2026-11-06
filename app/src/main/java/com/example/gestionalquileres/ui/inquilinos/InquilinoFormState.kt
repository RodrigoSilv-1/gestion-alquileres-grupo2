package com.example.gestionalquileres.ui.inquilinos

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.gestionalquileres.domain.model.Inquilino

/** Estado del formulario de inquilino. Sin argumentos = alta; con [inquilino] = edición. */
@Stable
class InquilinoFormState(inquilino: Inquilino? = null) {
    var nombre by mutableStateOf(inquilino?.nombre.orEmpty())
    var dni by mutableStateOf(inquilino?.dni.orEmpty())
    var telefono by mutableStateOf(inquilino?.telefono.orEmpty())
    var edad by mutableStateOf(inquilino?.edad?.takeIf { it > 0 }?.toString().orEmpty())
    var sexo by mutableStateOf(inquilino?.sexo.orEmpty())
    var sueldo by mutableStateOf(inquilino?.sueldo?.takeIf { it > 0.0 }?.toString().orEmpty())
    var fotografia by mutableStateOf(inquilino?.fotografia)

    /** Mínimo para guardar: nombre y DNI. */
    val esValido: Boolean
        get() = nombre.isNotBlank() && dni.isNotBlank()

    /** Si se pasa [base] se copia sobre ese inquilino (edición); si no, se crea uno nuevo. */
    fun toInquilino(base: Inquilino? = null): Inquilino = (base ?: Inquilino()).copy(
        nombre = nombre.trim(),
        dni = dni.trim(),
        telefono = telefono.trim(),
        edad = edad.toIntOrNull() ?: base?.edad ?: 0,
        sexo = sexo.trim(),
        fotografia = fotografia,
        sueldo = sueldo.toDoubleOrNull() ?: 0.0
    )
}