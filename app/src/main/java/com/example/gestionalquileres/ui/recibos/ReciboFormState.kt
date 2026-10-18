package com.example.gestionalquileres.ui.recibos

import com.example.gestionalquileres.domain.model.Contrato

data class ReciboFormState(
    val contratoSeleccionado: Contrato? = null,
    val agua: String = "",
    val luz: String = "",
    val mantenimiento: String = ""
)