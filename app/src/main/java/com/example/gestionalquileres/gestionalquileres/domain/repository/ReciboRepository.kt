package com.example.gestionalquileres.domain.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.CursorPagina
import com.example.gestionalquileres.domain.model.EstadoPago
import com.example.gestionalquileres.domain.model.FiltroRecibos
import com.example.gestionalquileres.domain.model.Pagina
import com.example.gestionalquileres.domain.model.Recibo
import com.example.gestionalquileres.domain.model.TAMANO_PAGINA

interface ReciboRepository {
    suspend fun generarYGuardarRecibo(reciboParcial: Recibo): AppResult<Recibo>

    /** Historial paginado, del más reciente al más antiguo, con filtros opcionales. */
    suspend fun obtenerPagina(
        filtro: FiltroRecibos = FiltroRecibos(),
        limite: Int = TAMANO_PAGINA,
        despues: CursorPagina? = null
    ): AppResult<Pagina<Recibo>>

    /** Control de cobros: marca un recibo como pagado o lo devuelve a pendiente. */
    suspend fun actualizarEstadoPago(idRecibo: String, nuevoEstado: EstadoPago): AppResult<Unit>
}