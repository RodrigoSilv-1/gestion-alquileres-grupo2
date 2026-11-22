package com.example.gestionalquileres.domain.model

data class Inmueble(
    val idInmueble: String = "",
    val distrito: String = "",
    val direccion: String = "",        // <-- Cambia de String? a String
    val numeroDireccion: String = "",  // <-- Cambia de String? a String
    val codigoPostal: String = "",
    val numeroPisos: Int = 0,
    val unidadesPorPiso: Int = 0,
    val fotografia: String = "",       // <-- Cambia de String? a String
    val active: Boolean = true
)