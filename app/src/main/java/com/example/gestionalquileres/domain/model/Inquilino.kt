package com.example.gestionalquileres.domain.model

data class Inquilino(
    val idInquilino: String = "",
    val nombre: String = "",
    val dni: String = "",
    val telefono: String = "",
    val edad: Int = 0,
    val sexo: String = "",
    val fotografia: String? = null,
    val active: Boolean = true,
    val sueldo: Double = 0.0 // <-- NUEVO: Indispensable para la evaluación de solvencia
)