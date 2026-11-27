package com.example.gestionalquileres.domain.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.CobranzaMes
import com.example.gestionalquileres.domain.model.IngresoMensual
import com.example.gestionalquileres.domain.model.ResumenGeneral

interface DashboardRepository {
    suspend fun obtenerResumenGeneral(desdeMes: String, hastaMes: String): AppResult<ResumenGeneral>
    suspend fun obtenerCobranzaMes(desde: String, hasta: String): AppResult<CobranzaMes>
    suspend fun obtenerIngresosUltimosMeses(cantidadMeses: Int): AppResult<List<IngresoMensual>>
}