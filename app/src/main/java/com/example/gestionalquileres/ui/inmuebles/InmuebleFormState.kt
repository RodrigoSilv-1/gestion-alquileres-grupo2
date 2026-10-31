package com.example.gestionalquileres.ui.inmuebles

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.gestionalquileres.domain.model.MAX_UNIDADES_POR_INMUEBLE
import com.example.gestionalquileres.domain.model.Inmueble

/** Estado del formulario de inmueble. Sin argumentos = alta; con [inmueble] = edición. */
@Stable
class InmuebleFormState(inmueble: Inmueble? = null) {
    var distrito by mutableStateOf(inmueble?.distrito.orEmpty())
    var direccion by mutableStateOf(inmueble?.direccion.orEmpty())
    var numeroDireccion by mutableStateOf(inmueble?.numeroDireccion.orEmpty())
    var codigoPostal by mutableStateOf(inmueble?.codigoPostal.orEmpty())
    var numeroPisos by mutableStateOf(inmueble?.numeroPisos?.toString().orEmpty())
    var unidadesPorPiso by mutableStateOf(inmueble?.unidadesPorPiso?.toString().orEmpty())
    var fotografia by mutableStateOf(inmueble?.fotografia)

    /** True si pisos × unidades supera el máximo. Se compara con Long para evitar desbordes. */
    val excedeLimite: Boolean
        get() {
            val pisos = numeroPisos.toLongOrNull() ?: 0L
            val unidades = unidadesPorPiso.toLongOrNull() ?: 0L
            return pisos > MAX_UNIDADES_POR_INMUEBLE ||
                    unidades > MAX_UNIDADES_POR_INMUEBLE ||
                    pisos * unidades > MAX_UNIDADES_POR_INMUEBLE
        }

    val totalUnidades: Int
        get() = (numeroPisos.toIntOrNull() ?: 0) * (unidadesPorPiso.toIntOrNull() ?: 0)

    /** En el alta se exige al menos 1 piso, 1 unidad por piso y no superar el límite. */
    fun esValido(esNuevo: Boolean): Boolean =
        distrito.isNotBlank() &&
                direccion.isNotBlank() &&
                codigoPostal.isNotBlank() &&
                (!esNuevo || (
                        (numeroPisos.toIntOrNull() ?: 0) > 0 &&
                                (unidadesPorPiso.toIntOrNull() ?: 0) > 0 &&
                                !excedeLimite
                        ))

    /** Si se pasa [base] se copia sobre ese inmueble (edición); si no, se crea uno nuevo. */
    fun toInmueble(base: Inmueble? = null): Inmueble = base?.copy(
        distrito = distrito.trim(),
        direccion = direccion.trim(),
        numeroDireccion = numeroDireccion.trim(),
        codigoPostal = codigoPostal.trim(),
        fotografia = fotografia
    ) ?: Inmueble(
        distrito = distrito.trim(),
        direccion = direccion.trim(),
        numeroDireccion = numeroDireccion.trim(),
        codigoPostal = codigoPostal.trim(),
        numeroPisos = numeroPisos.toIntOrNull() ?: 0,
        unidadesPorPiso = unidadesPorPiso.toIntOrNull() ?: 0,
        fotografia = fotografia
    )
}