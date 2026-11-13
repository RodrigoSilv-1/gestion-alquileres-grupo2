package com.example.gestionalquileres.ui.inquilinos

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.gestionalquileres.domain.model.Inquilino

const val LARGO_DNI = 8
const val LARGO_TELEFONO = 9
const val EDAD_MINIMA = 18
const val EDAD_MAXIMA = 99
const val LARGO_MINIMO_NOMBRE = 3

/** Deja solo dígitos y un separador decimal (se acepta "." o ","), con máximo 2 decimales. */
fun limpiarMonto(texto: String): String {
    val resultado = StringBuilder()
    var hayDecimal = false
    var decimales = 0
    for (c in texto) {
        when {
            c.isDigit() -> when {
                !hayDecimal -> resultado.append(c)
                decimales < 2 -> { resultado.append(c); decimales++ }
            }
            (c == '.' || c == ',') && !hayDecimal -> { hayDecimal = true; resultado.append('.') }
        }
    }
    return resultado.toString().take(10)
}

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

    private val sueldoNumerico: Double? get() = sueldo.toDoubleOrNull()
    private val edadNumerica: Int? get() = edad.toIntOrNull()

    // ---------- Mensajes de error (null = sin error o campo todavía vacío) ----------

    val errorNombre: String?
        get() = if (nombre.isNotBlank() && nombre.trim().length < LARGO_MINIMO_NOMBRE)
            "Ingresa el nombre completo." else null

    val errorDni: String?
        get() = if (dni.isNotEmpty() && dni.length != LARGO_DNI)
            "El DNI debe tener $LARGO_DNI dígitos (${dni.length}/$LARGO_DNI)." else null

    val errorTelefono: String?
        get() = when {
            telefono.isEmpty() -> null
            telefono.length != LARGO_TELEFONO || !telefono.startsWith("9") ->
                "El celular debe tener $LARGO_TELEFONO dígitos y empezar con 9."
            else -> null
        }

    val errorEdad: String?
        get() {
            val valor = edadNumerica ?: return null
            return if (valor !in EDAD_MINIMA..EDAD_MAXIMA)
                "La edad debe estar entre $EDAD_MINIMA y $EDAD_MAXIMA años." else null
        }

    val errorSueldo: String?
        get() {
            if (sueldo.isBlank()) return null
            val valor = sueldoNumerico ?: return "Ingresa un monto válido, por ejemplo 2500 o 2500.50."
            return if (valor <= 0.0) "El sueldo debe ser mayor a 0." else null
        }

    /** En edición el DNI está bloqueado, por eso no se valida. */
    fun esValido(esNuevo: Boolean): Boolean =
        nombre.trim().length >= LARGO_MINIMO_NOMBRE &&
                (!esNuevo || dni.length == LARGO_DNI) &&
                telefono.length == LARGO_TELEFONO && telefono.startsWith("9") &&
                (edadNumerica?.let { it in EDAD_MINIMA..EDAD_MAXIMA } == true) &&
                ((sueldoNumerico ?: 0.0) > 0.0)

    /** Si se pasa [base] se copia sobre ese inquilino (edición); si no, se crea uno nuevo. */
    fun toInquilino(base: Inquilino? = null): Inquilino = (base ?: Inquilino()).copy(
        nombre = nombre.trim(),
        dni = dni.trim(),
        telefono = telefono.trim(),
        edad = edadNumerica ?: base?.edad ?: 0,
        sexo = sexo.trim(),
        fotografia = fotografia,
        sueldo = sueldoNumerico ?: 0.0
    )
}