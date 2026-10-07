package com.example.gestionalquileres.ui.inquilinos

import com.example.gestionalquileres.domain.model.Inquilino

data class InquilinoUiState(
    val isLoading: Boolean = false,
    val inquilinos: List<Inquilino> = emptyList(),
    val inquilinoSeleccionado: Inquilino? = null,
    val inquilinoInactivoDetectado: Inquilino? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)