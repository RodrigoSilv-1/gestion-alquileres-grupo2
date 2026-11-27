package com.example.gestionalquileres.ui.inquilinos

import com.example.gestionalquileres.domain.model.Inquilino
import com.example.gestionalquileres.domain.normalizarBusqueda

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
    val errorMessage: String? = null,
    val fotoCompletaEnVista: String? = null,
    val cargandoFotoCompleta: Boolean = false
) {
    /** Lista que se muestra: `inquilinos` (completa) filtrada en memoria. */
    val inquilinosVisibles: List<Inquilino>
        get() {
            val consulta = filtroAplicado.normalizarBusqueda()
            return when {
                consulta.isEmpty() -> inquilinos
                tipoFiltroAplicado == TipoBusquedaInquilino.NOMBRE ->
                    inquilinos.filter { it.nombre.normalizarBusqueda().contains(consulta) }
                else ->
                    inquilinos.filter { it.dni.contains(consulta) }
            }
        }
}