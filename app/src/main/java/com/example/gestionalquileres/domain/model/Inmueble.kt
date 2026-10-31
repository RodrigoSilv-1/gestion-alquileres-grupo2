package com.example.gestionalquileres.domain.model

data class Inmueble(
    val idInmueble: String = "",
    val distrito:String = "",
    val direccion: String = "",
    val numeroDireccion: String = "",
    val codigoPostal: String = "",
    val numeroPisos: Int = 0,
    val unidadesPorPiso: Int = 0,
    val fotografia: String? = null,
    val active: Boolean = true
)

/** Máximo de unidades (numeroPisos × unidadesPorPiso) que se generan en un único lote de Firestore. */
const val MAX_UNIDADES_POR_INMUEBLE = 400