package com.example.gestionalquileres.ui.inquilinos

import com.example.gestionalquileres.domain.model.Inquilino

data class InquilinoUiState(
    val isLoading: Boolean = false,
    val inquilinos: List<Inquilino> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // Control de ventanas y navegación
    val inquilinoSeleccionado: Inquilino? = null, // Para la ventana de Modificar
    val inquilinoDarDeBaja: Inquilino? = null, // Para la confirmación de Baja
    val inquilinoInactivoDetectado: Inquilino? = null, // Para reactivación
    val mostrandoFormularioRegistro: Boolean = false, // Para mostrar el alta

    // Control de la barra de búsqueda
    val tipoBusqueda: String = "Nombre",
    val textoBusqueda: String = ""
)