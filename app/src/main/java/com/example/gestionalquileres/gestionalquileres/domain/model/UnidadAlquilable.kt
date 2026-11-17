package com.example.gestionalquileres.domain.model

data class UnidadAlquilable(
    val id: String = "",                    // ID único de la unidad (generado por Firestore)
    val inmuebleId: String = "",            // ID del inmueble al que pertenece
    val piso: Int = 0,                      // Número de piso (ej. 1, 2...)
    val numeroUnidad: Int = 0,              // Número de unidad en ese piso (ej. 1, 5...)
    val nombreFormateado: String = "",      // Ejemplo: "101"
    val estaOcupada: Boolean = false,
    val precioRenta: Double = 0.0
)