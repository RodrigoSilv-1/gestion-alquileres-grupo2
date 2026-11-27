package com.example.gestionalquileres.domain.model

/**
 * Filtros del historial de recibos. Fechas en formato yyyy-MM-dd (inclusivas).
 * Si hay [numero], la búsqueda es exacta por correlativo y se ignoran los demás filtros.
 */
data class FiltroRecibos(
    val idContrato: String? = null,
    val desde: String? = null,
    val hasta: String? = null,
    val numero: String? = null,
    val estado: EstadoPago? = null
) {
    val estaActivo: Boolean
        get() = idContrato != null || desde != null || hasta != null || numero != null || estado != null
}