package com.example.gestionalquileres.data.repository

import com.example.gestionalquileres.data.remote.ActualizarPrecioRequest
import com.example.gestionalquileres.data.remote.ApiService
import com.example.gestionalquileres.data.remote.UnidadAlquilableDto
import com.example.gestionalquileres.data.remote.safeApiCall
import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.UnidadAlquilable
import com.example.gestionalquileres.domain.repository.UnidadAlquilableRepository
import javax.inject.Inject

class UnidadAlquilableRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : UnidadAlquilableRepository {

    override suspend fun obtenerUnidadesPorInmueble(inmuebleId: String): AppResult<List<UnidadAlquilable>> {
        return safeApiCall {
            apiService.obtenerUnidadesPorInmueble("eq.$inmuebleId") // <-- ¡Vital el "eq."!
                .map { it.aDomain() }
        }
    }

    override suspend fun actualizarPrecioRenta(
        inmuebleId: String,
        unidadId: String,
        nuevoPrecio: Double
    ): AppResult<Unit> {
        if (inmuebleId.isBlank() || unidadId.isBlank()) {
            return AppResult.Error(Exception("Los identificadores son obligatorios."))
        }

        return safeApiCall {
            apiService.actualizarPrecioRenta(
                unidadIdFiltro = "eq.$unidadId",
                body = ActualizarPrecioRequest(nuevoPrecio)
            )
        }
    }

    // Helper privado de extensión para mapear DTO a Dominio
    private fun UnidadAlquilableDto.aDomain(): UnidadAlquilable {
        return UnidadAlquilable(
            id = id ?: "",
            inmuebleId = inmuebleId,
            piso = piso,
            numeroUnidad = numeroUnidad,
            nombreFormateado = nombreFormateado,
            estaOcupada = estaOcupada,
            precioRenta = precioRenta
        )
    }

    override suspend fun tieneUnidadesOcupadas(inmuebleId: String): AppResult<Boolean> {
        return safeApiCall {
            when (val resultado = obtenerUnidadesPorInmueble(inmuebleId)) {
                is AppResult.Exito -> resultado.valor.any { it.estaOcupada }
                is AppResult.Error -> throw resultado.excepcion
            }
        }
    }
}