package com.example.gestionalquileres.domain.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.CobranzaMes
import com.example.gestionalquileres.domain.model.ResumenGeneral

interface DashboardRepository {
    suspend fun obtenerResumenGeneral(
        fechaInicio: String,
        fechaFin: String
    ): AppResult<ResumenGeneral>

    suspend fun obtenerCobranzaMes(
        fechaInicio: String,
        fechaFin: String
    ): AppResult<CobranzaMes>
}