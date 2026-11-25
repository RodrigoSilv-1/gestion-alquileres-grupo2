package com.example.gestionalquileres.data.repository

import com.example.gestionalquileres.data.local.dao.InmuebleDao
import com.example.gestionalquileres.data.local.entity.InmuebleEntity
import com.example.gestionalquileres.data.network.NetworkMonitor
import com.example.gestionalquileres.data.remote.ApiService
import com.example.gestionalquileres.data.remote.InmuebleDto
import com.example.gestionalquileres.data.remote.UnidadAlquilableDto
import com.example.gestionalquileres.data.remote.safeApiCall
import com.example.gestionalquileres.data.sync.SyncManager
import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.CursorPagina
import com.example.gestionalquileres.domain.model.Inmueble
import com.example.gestionalquileres.domain.model.Pagina
import com.example.gestionalquileres.domain.repository.InmuebleRepository
import java.util.UUID
import javax.inject.Inject

class InmuebleRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val inmuebleDao: InmuebleDao,
    private val networkMonitor: NetworkMonitor,
    private val syncManager: SyncManager
) : InmuebleRepository {

    private data class CursorInmuebleId(val ultimoId: String) : CursorPagina

    override suspend fun registrar(inmueble: Inmueble): AppResult<Inmueble> {
        return safeApiCall {
            val idGenerado = inmueble.idInmueble.ifBlank { UUID.randomUUID().toString() }

            if (networkMonitor.isCurrentlyOnline()) {
                val dtoAEnviar = InmuebleDto(
                    id = idGenerado,
                    distrito = inmueble.distrito,
                    direccion = inmueble.direccion,
                    numeroDireccion = inmueble.numeroDireccion,
                    codigoPostal = inmueble.codigoPostal,
                    numeroPisos = inmueble.numeroPisos,
                    unidadesPorPiso = inmueble.unidadesPorPiso,
                    fotografia = inmueble.fotografia?: "",
                    active = true
                )

                val dtoCreado = apiService.registrarInmueble(dtoAEnviar).firstOrNull()
                val idNuevoInmueble = dtoCreado?.id?.takeIf { it.isNotBlank() } ?: idGenerado

                if (idNuevoInmueble.isNotBlank()) {
                    for (piso in 1..inmueble.numeroPisos) {
                        for (unidad in 1..inmueble.unidadesPorPiso) {
                            val numUnidad = (piso * 100) + unidad
                            val nuevaUnidadDto = UnidadAlquilableDto(
                                id = null,
                                inmuebleId = idNuevoInmueble,
                                piso = piso,
                                numeroUnidad = numUnidad,
                                nombreFormateado = "Depto. $numUnidad",
                                estaOcupada = false,
                                precioRenta = 0.0
                            )
                            apiService.registrarUnidad(nuevaUnidadDto)
                        }
                    }
                }

                val dominioCreado = (dtoCreado?.aDomain() ?: inmueble.copy(idInmueble = idNuevoInmueble, active = true))
                    .copy(idInmueble = idNuevoInmueble, active = true)

                inmuebleDao.upsert(dominioCreado.toEntity(syncStatus = "SYNCED"))
                if (idNuevoInmueble != idGenerado) {
                    runCatching { inmuebleDao.eliminarPorId(idGenerado) }
                }
                dominioCreado
            } else {
                val dominioOffline = inmueble.copy(idInmueble = idGenerado, active = true)
                val entityOffline = dominioOffline.toEntity(syncStatus = "PENDING_CREATE")
                val dtoOffline = InmuebleDto(
                    id = idGenerado,
                    distrito = dominioOffline.distrito,
                    direccion = dominioOffline.direccion,
                    numeroDireccion = dominioOffline.numeroDireccion,
                    codigoPostal = dominioOffline.codigoPostal,
                    numeroPisos = dominioOffline.numeroPisos,
                    unidadesPorPiso = dominioOffline.unidadesPorPiso,
                    fotografia = dominioOffline.fotografia?: "",
                    active = true
                )

                inmuebleDao.upsert(entityOffline)
                syncManager.encolarOperacion(
                    entityType = "INMUEBLE",
                    operationType = "CREATE",
                    entityId = idGenerado,
                    payload = dtoOffline
                )
                dominioOffline
            }
        }
    }

    override suspend fun obtenerTodos(): AppResult<List<Inmueble>> {
        return safeApiCall {
            if (networkMonitor.isCurrentlyOnline()) {
                runCatching { syncManager.sincronizarTodo() }
                val remotos = apiService.obtenerInmuebles().filter { it.active }
                val entidades = remotos.mapNotNull { dto ->
                    val id = dto.id ?: return@mapNotNull null
                    InmuebleEntity(
                        id = id,
                        distrito = dto.distrito,
                        direccion = dto.direccion,
                        numeroDireccion = dto.numeroDireccion,
                        codigoPostal = dto.codigoPostal,
                        numeroPisos = dto.numeroPisos,
                        unidadesPorPiso = dto.unidadesPorPiso,
                        fotografia = dto.fotografia,
                        active = dto.active,
                        syncStatus = "SYNCED"
                    )
                }
                inmuebleDao.upsertAll(entidades)
                val idsRemotos = entidades.map { it.id }
                if (idsRemotos.isNotEmpty()) {
                    inmuebleDao.eliminarObsoletosSincronizados(idsRemotos)
                }
            }
            inmuebleDao.obtenerActivos().map { it.toDomain() }
        }
    }

    override suspend fun obtenerPagina(
        limite: Int,
        despues: CursorPagina?
    ): AppResult<Pagina<Inmueble>> {
        return safeApiCall {
            val activos = when (val res = obtenerTodos()) {
                is AppResult.Exito -> res.valor.filter { it.active }
                is AppResult.Error -> throw res.excepcion
            }

            val ordenados = activos.sortedByDescending { it.idInmueble }
            val cursorId = (despues as? CursorInmuebleId)?.ultimoId
            val inicioIndex = if (cursorId != null) {
                val idx = ordenados.indexOfFirst { it.idInmueble == cursorId }
                if (idx >= 0) idx + 1 else 0
            } else {
                0
            }

            val tamano = limite.coerceAtLeast(1)
            val rebanada = if (inicioIndex < ordenados.size) {
                ordenados.drop(inicioIndex).take(tamano + 1)
            } else {
                emptyList()
            }

            val hayMas = rebanada.size > tamano
            val itemsPagina = if (hayMas) rebanada.take(tamano) else rebanada
            val siguienteCursor = if (hayMas && itemsPagina.isNotEmpty()) {
                CursorInmuebleId(ultimoId = itemsPagina.last().idInmueble)
            } else {
                null
            }

            Pagina(
                items = itemsPagina,
                siguiente = siguienteCursor
            )
        }
    }

    override suspend fun buscarPorDireccion(
        distrito: String,
        direccion: String,
        numeroDireccion: String,
        excluirId: String?
    ): AppResult<Inmueble?> {
        return safeApiCall {
            val listaActual = when (val res = obtenerTodos()) {
                is AppResult.Exito -> res.valor
                is AppResult.Error -> throw res.excepcion
            }

            val dNorm = distrito.trim().lowercase()
            val dirNorm = direccion.trim().lowercase()
            val numNorm = numeroDireccion.trim().lowercase()

            listaActual.firstOrNull { item ->
                (excluirId == null || item.idInmueble != excluirId) &&
                        item.distrito.trim().lowercase() == dNorm &&
                        item.direccion.trim().lowercase() == dirNorm &&
                        item.numeroDireccion.trim().lowercase() == numNorm
            }
        }
    }

    suspend fun obtenerPorCodigoPostal(codigoPostal: String): AppResult<List<Inmueble>> {
        return safeApiCall {
            if (networkMonitor.isCurrentlyOnline()) {
                apiService.obtenerInmuebles()
                    .filter { it.codigoPostal == codigoPostal && it.active }
                    .map { it.aDomain() }
            } else {
                inmuebleDao.obtenerActivos()
                    .filter { it.codigoPostal == codigoPostal && it.active }
                    .map { it.toDomain() }
            }
        }
    }

    suspend fun obtenerPorDistrito(distrito: String): AppResult<List<Inmueble>> {
        return safeApiCall {
            if (networkMonitor.isCurrentlyOnline()) {
                apiService.obtenerInmuebles()
                    .filter { it.active }
                    .map { it.aDomain() }
                    .filter { it.distrito.contains(distrito, ignoreCase = true) }
            } else {
                inmuebleDao.obtenerActivos()
                    .filter { it.active && it.distrito.contains(distrito, ignoreCase = true) }
                    .map { it.toDomain() }
            }
        }
    }

    override suspend fun actualizar(inmueble: Inmueble): AppResult<Inmueble> {
        require(inmueble.idInmueble.isNotBlank()) { "El ID del inmueble es obligatorio." }

        return safeApiCall {
            val dtoAEnviar = InmuebleDto(
                id = inmueble.idInmueble,
                distrito = inmueble.distrito,
                direccion = inmueble.direccion,
                numeroDireccion = inmueble.numeroDireccion,
                codigoPostal = inmueble.codigoPostal,
                numeroPisos = inmueble.numeroPisos,
                unidadesPorPiso = inmueble.unidadesPorPiso,
                fotografia = inmueble.fotografia?: "",
                active = inmueble.active
            )

            if (networkMonitor.isCurrentlyOnline()) {
                val dtoActualizado = apiService.actualizarInmueble(
                    idFiltro = "eq.${inmueble.idInmueble}",
                    inmueble = dtoAEnviar
                ).first()

                val dominioActualizado = dtoActualizado.aDomain()
                inmuebleDao.upsert(dominioActualizado.toEntity(syncStatus = "SYNCED"))
                dominioActualizado
            } else {
                inmuebleDao.upsert(inmueble.toEntity(syncStatus = "PENDING_UPDATE"))
                syncManager.encolarOperacion(
                    entityType = "INMUEBLE",
                    operationType = "UPDATE",
                    entityId = inmueble.idInmueble,
                    payload = dtoAEnviar
                )
                inmueble
            }
        }
    }

    override suspend fun darDeBaja(idInmueble: String): AppResult<Unit> {
        require(idInmueble.isNotBlank()) { "El ID del inmueble es obligatorio." }

        return safeApiCall {
            if (networkMonitor.isCurrentlyOnline()) {
                apiService.darDeBajaInmueble(idFiltro = "eq.$idInmueble")
                val actual = inmuebleDao.obtenerActivos().firstOrNull { it.id == idInmueble }
                if (actual != null) {
                    inmuebleDao.upsert(actual.copy(active = false, syncStatus = "SYNCED"))
                }
            } else {
                val actual = inmuebleDao.obtenerActivos().firstOrNull { it.id == idInmueble }
                    ?: throw IllegalStateException("No se encontró el inmueble local para dar de baja offline.")
                val inactivoEntity = actual.copy(active = false, syncStatus = "PENDING_UPDATE")
                inmuebleDao.upsert(inactivoEntity)
                val dtoInactivo = InmuebleDto(
                    id = inactivoEntity.id,
                    distrito = inactivoEntity.distrito,
                    direccion = inactivoEntity.direccion,
                    numeroDireccion = inactivoEntity.numeroDireccion,
                    codigoPostal = inactivoEntity.codigoPostal,
                    numeroPisos = inactivoEntity.numeroPisos,
                    unidadesPorPiso = inactivoEntity.unidadesPorPiso,
                    fotografia = inactivoEntity.fotografia ?: "",
                    active = false
                )
                syncManager.encolarOperacion(
                    entityType = "INMUEBLE",
                    operationType = "UPDATE",
                    entityId = idInmueble,
                    payload = dtoInactivo
                )
            }
        }
    }

    override suspend fun reactivarYActualizar(inmueble: Inmueble): AppResult<Inmueble> {
        require(inmueble.idInmueble.isNotBlank()) { "El ID del inmueble es obligatorio." }
        val reactivado = inmueble.copy(active = true)

        return safeApiCall {
            val dtoAEnviar = InmuebleDto(
                id = reactivado.idInmueble,
                distrito = reactivado.distrito,
                direccion = reactivado.direccion,
                numeroDireccion = reactivado.numeroDireccion,
                codigoPostal = reactivado.codigoPostal,
                numeroPisos = reactivado.numeroPisos,
                unidadesPorPiso = reactivado.unidadesPorPiso,
                fotografia = reactivado.fotografia?: "",
                active = true
            )

            if (networkMonitor.isCurrentlyOnline()) {
                val dtoActualizado = apiService.actualizarInmueble(
                    idFiltro = "eq.${reactivado.idInmueble}",
                    inmueble = dtoAEnviar
                ).first()

                val dominioActualizado = dtoActualizado.aDomain()
                inmuebleDao.upsert(dominioActualizado.toEntity(syncStatus = "SYNCED"))
                dominioActualizado
            } else {
                inmuebleDao.upsert(reactivado.toEntity(syncStatus = "PENDING_UPDATE"))
                syncManager.encolarOperacion(
                    entityType = "INMUEBLE",
                    operationType = "UPDATE",
                    entityId = reactivado.idInmueble,
                    payload = dtoAEnviar
                )
                reactivado
            }
        }
    }

    private fun InmuebleDto.aDomain(): Inmueble {
        return Inmueble(
            idInmueble = id ?: "",
            distrito = distrito,
            direccion = direccion,
            numeroDireccion = numeroDireccion,
            codigoPostal = codigoPostal,
            numeroPisos = numeroPisos,
            unidadesPorPiso = unidadesPorPiso,
            fotografia = fotografia ?: "",
            active = active
        )
    }

    private fun InmuebleEntity.toDomain(): Inmueble {
        return Inmueble(
            idInmueble = id,
            distrito = distrito,
            direccion = direccion,
            numeroDireccion = numeroDireccion,
            codigoPostal = codigoPostal,
            numeroPisos = numeroPisos,
            unidadesPorPiso = unidadesPorPiso,
            fotografia = fotografia ?: "",
            active = active
        )
    }

    private fun Inmueble.toEntity(syncStatus: String): InmuebleEntity {
        return InmuebleEntity(
            id = idInmueble,
            distrito = distrito,
            direccion = direccion,
            numeroDireccion = numeroDireccion,
            codigoPostal = codigoPostal,
            numeroPisos = numeroPisos,
            unidadesPorPiso = unidadesPorPiso,
            fotografia = fotografia,
            active = active,
            syncStatus = syncStatus
        )
    }
}