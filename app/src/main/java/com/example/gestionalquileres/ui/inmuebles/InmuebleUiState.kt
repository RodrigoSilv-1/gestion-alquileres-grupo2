package com.example.gestionalquileres.ui.inmuebles

import com.example.gestionalquileres.domain.model.Inmueble
import com.example.gestionalquileres.domain.model.UnidadAlquilable

enum class TipoBusqueda(val etiqueta: String) {
    DISTRITO("Distrito"),
    CODIGO_POSTAL("Código Postal")
}

data class InmuebleUiState(
    val inmuebles: List<Inmueble> = emptyList(),
    val isLoading: Boolean = false,

    // Navegación interna de la pantalla
    val mostrandoFormularioRegistro: Boolean = false,
    val inmuebleSeleccionado: Inmueble? = null,       // Para editar
    val inmuebleEnDetalle: Inmueble? = null,          // Para ver unidades y precios
    val inmuebleDarDeBaja: Inmueble? = null,          // Alerta de baja
    val inmuebleInactivoDetectado: Inmueble? = null,  // Alerta de reactivación

    // Detalle del inmueble
    val unidadesAlquilables: List<UnidadAlquilable> = emptyList(),
    val cargandoUnidades: Boolean = false,

    // Buscador
    val tipoBusqueda: TipoBusqueda = TipoBusqueda.DISTRITO,
    val textoBusqueda: String = "",

    // Mensajes
    val successMessage: String? = null,
    val errorMessage: String? = null
)