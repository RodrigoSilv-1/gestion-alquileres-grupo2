package com.example.gestionalquileres.domain.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Contrato

interface ContratoRepository {
    suspend fun registrarContrato(contrato: Contrato): AppResult<Contrato>
    suspend fun obtenerContratosActivos(): AppResult<List<Contrato>>
    suspend fun obtenerContratoActivoPorInquilino(inquilinoId: String): AppResult<Contrato?>
    suspend fun finalizarContrato(idContrato: String, inmuebleId: String, unidadId: String): AppResult<Unit>

    /** Una página de contratos activos, del más reciente al más antiguo. */
    suspend fun obtenerContratosActivosPagina(
        limite: Int = TAMANO_PAGINA,
        despues: CursorPagina? = null
    ): AppResult<Pagina<Contrato>>

    /** MIGRACIÓN ÚNICA: reconstruye las marcas de ocupación a partir de los contratos vigentes. */
    suspend fun reconstruirOcupacion(): AppResult<Int>
}