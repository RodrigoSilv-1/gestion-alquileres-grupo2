package com.example.gestionalquileres.ui.inquilinos

import com.example.gestionalquileres.domain.model.Inquilino

enum class TipoBusquedaInquilino(val etiqueta: String) {
    NOMBRE("Nombre"),
    DNI("DNI")
}

data class InquilinoUiState(
    val inquilinos: List<Inquilino> = emptyList(),
    val isLoading: Boolean = false,

    // Navegación interna de la pantalla
    val mostrandoFormularioRegistro: Boolean = false,
    val inquilinoSeleccionado: Inquilino? = null,       // Para editar
    val inquilinoDarDeBaja: Inquilino? = null,          // Alerta de baja
    val inquilinoInactivoDetectado: Inquilino? = null,  // Alerta de reactivación

    // Buscador
    val tipoBusqueda: TipoBusquedaInquilino = TipoBusquedaInquilino.NOMBRE,
    val textoBusqueda: String = "",
    val filtroAplicado: String = "",                    // texto con el que se pulsó "Buscar"
    val tipoFiltroAplicado: TipoBusquedaInquilino = TipoBusquedaInquilino.NOMBRE,

    // Mensajes
    val successMessage: String? = null,
    val errorMessage: String? = null
) {
    /** Lista que se muestra: `inquilinos` (completa) filtrada en memoria. */
    val inquilinosVisibles: List<Inquilino>
        get() = when {
            filtroAplicado.isBlank() -> inquilinos
            tipoFiltroAplicado == TipoBusquedaInquilino.NOMBRE ->
                inquilinos.filter { it.nombre.contains(filtroAplicado, ignoreCase = true) }
            else -> inquilinos.filter { it.dni == filtroAplicado }
        }
}