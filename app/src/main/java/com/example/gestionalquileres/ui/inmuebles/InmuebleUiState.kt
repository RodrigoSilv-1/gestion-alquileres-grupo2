package com.example.gestionalquileres.ui.inmuebles

import com.example.gestionalquileres.domain.model.Inmueble

data class InmuebleUiState(
    val inmuebles: List<Inmueble> = emptyList(),
    val isLoading: Boolean = false,

    // Control de ventanas y formularios
    val mostrandoFormularioRegistro: Boolean = false,
    val inmuebleSeleccionado: Inmueble? = null, // Para editar
    val inmuebleDarDeBaja: Inmueble? = null, // Para la alerta de eliminar
    val inmuebleInactivoDetectado: Inmueble? = null, // Para reactivar

    // Buscador
    val tipoBusqueda: String = "Distrito",
    val textoBusqueda: String = "",

    // Mensajes
    val successMessage: String? = null,
    val errorMessage: String? = null
)