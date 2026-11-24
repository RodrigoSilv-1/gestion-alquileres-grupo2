package com.example.gestionalquileres.ui.contrato

const val DURACION_MINIMA_MESES = 3L
data class ContratoFormState(
    val fechaInicio: String = "",
    val fechaFin: String = ""
)