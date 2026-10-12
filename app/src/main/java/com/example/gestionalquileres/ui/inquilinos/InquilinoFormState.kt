package com.example.gestionalquileres.ui.inquilinos

data class InquilinoFormState(
    val nombre: String = "",
    val dni: String = "",
    val edad: String = "",
    val sexo: String = "",
    val telefono: String = "",
    val sueldo: String = "",
    val fotografiaBase64: String? = null
)