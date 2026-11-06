package com.example.gestionalquileres.domain.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Recibo

interface ReciboRepository {
    suspend fun generarYGuardarRecibo(reciboParcial: Recibo): AppResult<Recibo>
    suspend fun obtenerTodosLosRecibos(): AppResult<List<Recibo>>
}