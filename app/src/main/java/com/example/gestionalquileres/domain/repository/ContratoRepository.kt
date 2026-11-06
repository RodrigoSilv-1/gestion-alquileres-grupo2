package com.example.gestionalquileres.domain.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Contrato

interface ContratoRepository {
    /** Registra el contrato y marca la unidad como ocupada (atómico). */
    suspend fun registrarContrato(contrato: Contrato): AppResult<Contrato>

    suspend fun obtenerContratosActivos(): AppResult<List<Contrato>>
    suspend fun obtenerContratoActivoPorInquilino(inquilinoId: String): AppResult<Contrato?>

    /** Finaliza el contrato y libera la unidad (atómico). */
    suspend fun finalizarContrato(idContrato: String, inmuebleId: String, unidadId: String): AppResult<Unit>
}