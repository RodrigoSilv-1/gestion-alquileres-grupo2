package com.example.gestionalquileres.data.repository

import android.content.Context
import com.example.gestionalquileres.data.network.NetworkMonitor
import com.example.gestionalquileres.data.remote.ActualizarOcupacionRequest
import com.example.gestionalquileres.data.remote.ApiService
import com.example.gestionalquileres.data.remote.ContratoDto
import com.example.gestionalquileres.data.remote.safeApiCall
import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Contrato
import com.example.gestionalquileres.domain.repository.ContratoRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class ContratoRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val networkMonitor: NetworkMonitor,
    private val gson: Gson,

    @ApplicationContext private val context: Context
) : ContratoRepository {

    private val prefs by lazy {
        context.getSharedPreferences("contratos_cache_prefs", Context.MODE_PRIVATE)
    }
    private val cacheMutex = Mutex()
    private var contratosEnMemoria: List<Contrato>? = null

    companion object {
        private const val KEY_CONTRATOS_JSON = "contratos_cache_json"
    }

    private suspend fun guardarCache(lista: List<Contrato>) {
        cacheMutex.withLock {
            contratosEnMemoria = lista
            prefs.edit().putString(KEY_CONTRATOS_JSON, gson.toJson(lista)).apply()
        }
    }

    private suspend fun leerCache(): List<Contrato> {
        cacheMutex.withLock {
            contratosEnMemoria?.let { return it }
            val json = prefs.getString(KEY_CONTRATOS_JSON, null) ?: return emptyList()
            val tipo = object : TypeToken<List<Contrato>>() {}.type
            val recuperada: List<Contrato> = runCatching {
                gson.fromJson<List<Contrato>>(json, tipo)
            }.getOrNull() ?: emptyList()
            contratosEnMemoria = recuperada
            return recuperada
        }
    }

    override suspend fun registrarContrato(contrato: Contrato): AppResult<Contrato> {
        return safeApiCall {
            val dtoAEnviar = ContratoDto(
                id = null,
                inmuebleId = contrato.inmuebleId,
                direccionInmueble = contrato.direccionInmueble,
                distritoInmueble = contrato.distritoInmueble,
                unidadId = contrato.unidadId,
                nombreUnidad = contrato.nombreUnidad,
                inquilinoId = contrato.inquilinoId,
                nombreInquilino = contrato.nombreInquilino,
                dniInquilino = contrato.dniInquilino,
                precioRentaAcordado = contrato.precioRentaAcordado,
                fechaInicio = contrato.fechaInicio,
                fechaFin = contrato.fechaFin,
                active = true
            )

            val dtoCreado = apiService.registrarContrato(dtoAEnviar).first()

            if (contrato.unidadId.isNotBlank()) {
                apiService.actualizarEstadoOcupacion(
                    unidadIdFiltro = "eq.${contrato.unidadId}",
                    body = ActualizarOcupacionRequest(estaOcupada = true)
                )
            }

            val creadoDominio = dtoCreado.aDomain()
            val actualCache = leerCache().toMutableList()
            actualCache.removeAll {
                it.idContrato == creadoDominio.idContrato ||
                        (it.unidadId == creadoDominio.unidadId && it.active)
            }
            actualCache.add(creadoDominio)
            guardarCache(actualCache)
            creadoDominio
        }
    }

    override suspend fun obtenerContratosActivos(): AppResult<List<Contrato>> {
        return safeApiCall {
            if (networkMonitor.isCurrentlyOnline()) {
                val remotos = apiService.obtenerContratos()
                val todosDominio = remotos.map { it.aDomain() }
                guardarCache(todosDominio)
                todosDominio.filter { it.active }
            } else {
                leerCache().filter { it.active }
            }
        }
    }

    override suspend fun obtenerContratoActivoPorInquilino(inquilinoId: String): AppResult<Contrato?> {
        return safeApiCall {
            if (networkMonitor.isCurrentlyOnline()) {
                val remotos = apiService.obtenerContratos()
                val todosDominio = remotos.map { it.aDomain() }
                guardarCache(todosDominio)
                todosDominio.firstOrNull { it.inquilinoId == inquilinoId && it.active }
            } else {
                leerCache().firstOrNull { it.inquilinoId == inquilinoId && it.active }
            }
        }
    }

    override suspend fun obtenerHistorialCompleto(): AppResult<List<Contrato>> {
        return safeApiCall {
            if (networkMonitor.isCurrentlyOnline()) {
                val remotos = apiService.obtenerContratos()
                val todosDominio = remotos.map { it.aDomain() }
                guardarCache(todosDominio)
                todosDominio
            } else {
                leerCache()
            }
        }
    }

    override suspend fun finalizarContrato(
        idContrato: String,
        inmuebleId: String,
        unidadId: String
    ): AppResult<Unit> {
        if (idContrato.isBlank()) {
            return AppResult.Error(Exception("El ID del contrato es obligatorio."))
        }

        return safeApiCall {
            apiService.finalizarContrato(idFiltro = "eq.$idContrato")

            if (unidadId.isNotBlank()) {
                apiService.actualizarEstadoOcupacion(
                    unidadIdFiltro = "eq.$unidadId",
                    body = ActualizarOcupacionRequest(estaOcupada = false)
                )
            }

            val actualizada = leerCache().map { contrato ->
                if (contrato.idContrato == idContrato) {
                    contrato.copy(active = false)
                } else {
                    contrato
                }
            }
            guardarCache(actualizada)
        }
    }

    private fun ContratoDto.aDomain(): Contrato {
        return Contrato(
            idContrato = id ?: "",
            inmuebleId = inmuebleId,
            direccionInmueble = direccionInmueble,
            distritoInmueble = distritoInmueble,
            unidadId = unidadId,
            nombreUnidad = nombreUnidad,
            inquilinoId = inquilinoId,
            nombreInquilino = nombreInquilino,
            dniInquilino = dniInquilino,
            precioRentaAcordado = precioRentaAcordado,
            fechaInicio = fechaInicio,
            fechaFin = fechaFin,
            active = active
        )
    }
}