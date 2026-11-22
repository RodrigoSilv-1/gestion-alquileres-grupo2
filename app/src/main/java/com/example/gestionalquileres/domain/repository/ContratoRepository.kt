package com.example.gestionalquileres.domain.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Contrato

interface ContratoRepository {
    suspend fun registrarContrato(contrato: Contrato): AppResult<Contrato>
    suspend fun obtenerContratosActivos(): AppResult<List<Contrato>>
    suspend fun obtenerContratoActivoPorInquilino(inquilinoId: String): AppResult<Contrato?>
    suspend fun obtenerHistorialCompleto(): AppResult<List<Contrato>>
    suspend fun finalizarContrato(idContrato: String, inmuebleId: String, unidadId: String): AppResult<Unit>
}