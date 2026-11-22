package com.example.gestionalquileres.data.repository

import com.example.gestionalquileres.data.local.dao.InquilinoDao
import com.example.gestionalquileres.data.local.entity.InquilinoEntity
import com.example.gestionalquileres.data.network.NetworkMonitor
import com.example.gestionalquileres.data.remote.ApiService
import com.example.gestionalquileres.data.remote.InquilinoDto
import com.example.gestionalquileres.data.remote.safeApiCall
import com.example.gestionalquileres.data.sync.SyncManager
import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Inquilino
import com.example.gestionalquileres.domain.repository.InquilinoRepository
import java.util.UUID
import javax.inject.Inject

class InquilinoRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val inquilinoDao: InquilinoDao,
    private val networkMonitor: NetworkMonitor,
    private val syncManager: SyncManager
) : InquilinoRepository {

    override suspend fun registrar(inquilino: Inquilino): AppResult<Inquilino> {
        return safeApiCall {
            val idGenerado = inquilino.idInquilino.ifBlank { UUID.randomUUID().toString() }

            if (networkMonitor.isCurrentlyOnline()) {
                val dtoAEnviar = InquilinoDto(
                    idInquilino = idGenerado,
                    nombre = inquilino.nombre,
                    dni = inquilino.dni,
                    telefono = inquilino.telefono,
                    edad = inquilino.edad,
                    sexo = inquilino.sexo,
                    fotografia = inquilino.fotografia,
                    active = true,
                    sueldo = inquilino.sueldo
                )

                val dtoCreado = apiService.registrarInquilino(dtoAEnviar).firstOrNull() ?: dtoAEnviar
                val dominioCreado = dtoCreado.toDomain(idFallback = idGenerado)
                inquilinoDao.upsert(dominioCreado.toEntity(syncStatus = "SYNCED"))
                if (dominioCreado.idInquilino != idGenerado) {
                    runCatching { inquilinoDao.eliminarPorId(idGenerado) }
                }
                dominioCreado
            } else {
                val dominioOffline = inquilino.copy(idInquilino = idGenerado, active = true)
                val entityOffline = dominioOffline.toEntity(syncStatus = "PENDING_CREATE")
                inquilinoDao.upsert(entityOffline)
                syncManager.encolarOperacion(
                    entityType = "INQUILINO",
                    operationType = "CREATE",
                    entityId = idGenerado,
                    payload = entityOffline
                )
                dominioOffline
            }
        }
    }

    override suspend fun obtenerTodos(): AppResult<List<Inquilino>> {
        return safeApiCall {
            if (networkMonitor.isCurrentlyOnline()) {
                runCatching { syncManager.sincronizarTodo() }
                val remotosActivos = apiService.obtenerInquilinos().filter { it.active }
                val entidades = remotosActivos.mapIndexedNotNull { idx, dto ->
                    val id = dto.idInquilino?.takeIf { it.isNotBlank() } ?: "remoto_${dto.dni}_$idx"
                    dto.toDomain(idFallback = id).toEntity(syncStatus = "SYNCED")
                }
                inquilinoDao.upsertAll(entidades)
                val idsRemotos = entidades.map { it.idInquilino }
                if (idsRemotos.isNotEmpty()) {
                    inquilinoDao.eliminarObsoletosSincronizados(idsRemotos)
                }
            }
            inquilinoDao.obtenerActivos().map { it.toDomain() }
        }
    }

    override suspend fun obtenerPorDni(dni: String): AppResult<List<Inquilino>> {
        return safeApiCall {
            if (networkMonitor.isCurrentlyOnline()) {
                val listaDto = apiService.obtenerInquilinos()
                listaDto
                    .filter { it.dni == dni && it.active }
                    .map { it.toDomain() }
            } else {
                inquilinoDao.obtenerActivos()
                    .filter { it.dni == dni }
                    .map { it.toDomain() }
            }
        }
    }

    override suspend fun obtenerPorNombre(nombre: String): AppResult<List<Inquilino>> {
        return safeApiCall {
            if (networkMonitor.isCurrentlyOnline()) {
                val listaDto = apiService.obtenerInquilinos()
                listaDto
                    .filter { it.active && it.nombre.contains(nombre, ignoreCase = true) }
                    .map { it.toDomain() }
            } else {
                inquilinoDao.obtenerActivos()
                    .filter { it.nombre.contains(nombre, ignoreCase = true) }
                    .map { it.toDomain() }
            }
        }
    }

    override suspend fun actualizar(inquilino: Inquilino): AppResult<Inquilino> {
        if (inquilino.idInquilino.isBlank()) {
            return AppResult.Error(Exception("El ID del inquilino es obligatorio."))
        }

        return safeApiCall {
            if (networkMonitor.isCurrentlyOnline()) {
                val dtoAEnviar = InquilinoDto(
                    idInquilino = inquilino.idInquilino,
                    nombre = inquilino.nombre,
                    dni = inquilino.dni,
                    telefono = inquilino.telefono,
                    edad = inquilino.edad,
                    sexo = inquilino.sexo,
                    fotografia = inquilino.fotografia,
                    active = inquilino.active,
                    sueldo = inquilino.sueldo
                )

                val dtoActualizado = apiService.actualizarInquilino(
                    idFiltro = "eq.${inquilino.idInquilino}",
                    inquilino = dtoAEnviar
                ).firstOrNull() ?: dtoAEnviar

                val dominioActualizado = dtoActualizado.toDomain(idFallback = inquilino.idInquilino)
                inquilinoDao.upsert(dominioActualizado.toEntity(syncStatus = "SYNCED"))
                dominioActualizado
            } else {
                val entityOffline = inquilino.toEntity(syncStatus = "PENDING_UPDATE")
                inquilinoDao.upsert(entityOffline)
                syncManager.encolarOperacion(
                    entityType = "INQUILINO",
                    operationType = "UPDATE",
                    entityId = inquilino.idInquilino,
                    payload = entityOffline
                )
                inquilino
            }
        }
    }

    override suspend fun darDeBaja(idInquilino: String): AppResult<Unit> {
        if (idInquilino.isBlank()) {
            return AppResult.Error(Exception("El ID del inquilino es obligatorio."))
        }

        return safeApiCall {
            val actual = inquilinoDao.obtenerActivos().firstOrNull { it.idInquilino == idInquilino }
            if (networkMonitor.isCurrentlyOnline()) {
                apiService.darDeBajaInquilino(idFiltro = "eq.$idInquilino")
                if (actual != null) {
                    inquilinoDao.upsert(actual.copy(active = false, syncStatus = "SYNCED"))
                } else {
                    inquilinoDao.eliminarPorId(idInquilino)
                }
            } else {
                val baseEntity = actual
                    ?: throw IllegalStateException("No se encontró el inquilino local para dar de baja offline.")
                val inactivoEntity = baseEntity.copy(active = false, syncStatus = "PENDING_UPDATE")
                inquilinoDao.upsert(inactivoEntity)
                syncManager.encolarOperacion(
                    entityType = "INQUILINO",
                    operationType = "UPDATE",
                    entityId = idInquilino,
                    payload = inactivoEntity
                )
            }
        }
    }

    override suspend fun buscarPorDniCualquiera(dni: String): AppResult<Inquilino?> {
        return safeApiCall {
            if (networkMonitor.isCurrentlyOnline()) {
                val listaDto = apiService.obtenerInquilinos()
                listaDto.firstOrNull { it.dni == dni }?.toDomain()
            } else {
                inquilinoDao.obtenerActivos()
                    .firstOrNull { it.dni == dni }
                    ?.toDomain()
            }
        }
    }

    override suspend fun reactivarYActualizar(inquilino: Inquilino): AppResult<Inquilino> {
        return actualizar(inquilino.copy(active = true))
    }

    private fun InquilinoDto.toDomain(idFallback: String = ""): Inquilino = Inquilino(
        idInquilino = idInquilino?.takeIf { it.isNotBlank() } ?: idFallback,
        nombre = nombre,
        dni = dni,
        telefono = telefono,
        edad = edad,
        sexo = sexo,
        fotografia = fotografia,
        active = active,
        sueldo = sueldo
    )

    private fun InquilinoEntity.toDomain(): Inquilino = Inquilino(
        idInquilino = idInquilino,
        nombre = nombre,
        dni = dni,
        telefono = telefono,
        edad = edad,
        sexo = sexo,
        fotografia = fotografia,
        active = active,
        sueldo = sueldo
    )

    private fun Inquilino.toEntity(syncStatus: String): InquilinoEntity = InquilinoEntity(
        idInquilino = idInquilino,
        nombre = nombre,
        dni = dni,
        telefono = telefono,
        edad = edad,
        sexo = sexo,
        fotografia = fotografia ?: "",
        active = active,
        sueldo = sueldo,
        syncStatus = syncStatus
    )
}