package com.example.gestionalquileres.domain.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.UnidadAlquilable

interface UnidadAlquilableRepository {
    suspend fun obtenerUnidadesPorInmueble(inmuebleId: String): AppResult<List<UnidadAlquilable>>
    suspend fun tieneUnidadesOcupadas(inmuebleId: String): AppResult<Boolean>
    suspend fun actualizarPrecioRenta(inmuebleId: String, unidadId: String, nuevoPrecio: Double): AppResult<Unit>
}