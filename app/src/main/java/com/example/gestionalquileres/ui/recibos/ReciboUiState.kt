package com.example.gestionalquileres.ui.recibos

import com.example.gestionalquileres.domain.model.Contrato
import com.example.gestionalquileres.domain.model.CursorPagina
import com.example.gestionalquileres.domain.model.FiltroRecibos
import com.example.gestionalquileres.domain.model.Recibo

data class ReciboUiState(
    // Estados generales
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // Datos principales
    val recibos: List<Recibo> = emptyList(),
    val contratosActivos: List<Contrato> = emptyList(),

    // Paginación y filtros del historial
    val filtro: FiltroRecibos = FiltroRecibos(),
    val siguientePagina: CursorPagina? = null,
    val cargandoMas: Boolean = false,

    // Recibo cuyo estado de pago se está actualizando (evita doble toque)
    val idReciboActualizando: String? = null,

    // Controles de UI y filtros rápidos
    val mostrandoFormularioRegistro: Boolean = false,
    val textoBusqueda: String = "",
    val filtroEstado: String = FILTRO_TODOS
) {
    companion object {
        const val FILTRO_TODOS = "Todos"
        const val FILTRO_PENDIENTES = "Pendientes"
        const val FILTRO_COBRADOS = "Cobrados"
    }

    val hayMas: Boolean
        get() = siguientePagina != null

    val recibosFiltrados: List<Recibo>
        get() {
            val texto = textoBusqueda.trim()

            return recibos.filter { recibo ->
                val coincideEstado = when (filtroEstado) {
                    FILTRO_PENDIENTES -> recibo.estadoPago == Recibo.ESTADO_PENDIENTE
                    FILTRO_COBRADOS -> recibo.estadoPago == Recibo.ESTADO_COBRADO
                    else -> true
                }

                val coincideBusqueda = texto.isBlank() ||
                        recibo.numeroUnico.contains(other = texto, ignoreCase = true) ||
                        recibo.idContrato.contains(other = texto, ignoreCase = true)

                coincideEstado && coincideBusqueda
            }
        }
}