package com.example.gestionalquileres.ui.inmuebles

import com.example.gestionalquileres.domain.model.Inmueble

data class InmuebleUiState(
    val isLoading: Boolean = false,
    val inmuebles: List<Inmueble> = emptyList(),
    val inmuebleSeleccionado: Inmueble? = null,
    val inmuebleInactivoDetectado: Inmueble? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)