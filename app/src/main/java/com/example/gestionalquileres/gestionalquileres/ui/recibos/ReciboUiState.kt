package com.example.gestionalquileres.ui.recibos

import com.example.gestionalquileres.domain.model.Contrato
import com.example.gestionalquileres.domain.model.Recibo
import com.example.gestionalquileres.domain.model.CursorPagina
import com.example.gestionalquileres.domain.model.FiltroRecibos

data class ReciboUiState(
    // Estados generales
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // Datos principales
    val recibos: List<Recibo> = emptyList(), // Historial de recibos generados
    val contratosActivos: List<Contrato> = emptyList(), // Para el dropdown de "A quién cobrar"

    // Paginación y filtros del historial
    val filtro: FiltroRecibos = FiltroRecibos(),
    val siguientePagina: CursorPagina? = null,
    val cargandoMas: Boolean = false,

    // Recibo cuyo estado de pago se está actualizando (evita doble toque)
    val idReciboActualizando: String? = null,

    // Controles de navegación
    val mostrandoFormularioRegistro: Boolean = false,

    // Búsqueda por número exacto
    val textoBusqueda: String = ""
) {
    val hayMas: Boolean get() = siguientePagina != null
}
