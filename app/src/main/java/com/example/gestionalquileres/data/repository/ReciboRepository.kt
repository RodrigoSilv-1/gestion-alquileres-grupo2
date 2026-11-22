package com.example.gestionalquileres.data.repository

import com.example.gestionalquileres.data.local.dao.ReciboDao
import com.example.gestionalquileres.data.local.entity.ReciboEntity
import com.example.gestionalquileres.data.network.NetworkMonitor
import com.example.gestionalquileres.data.remote.ActualizarEstadoRequest
import com.example.gestionalquileres.data.remote.ApiService
import com.example.gestionalquileres.data.remote.ReciboDto
import com.example.gestionalquileres.data.remote.safeApiCall
import com.example.gestionalquileres.data.sync.SyncManager
import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Recibo
import com.example.gestionalquileres.domain.repository.ReciboRepository
import java.util.UUID
import javax.inject.Inject

class ReciboRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val reciboDao: ReciboDao,
    private val networkMonitor: NetworkMonitor,
    private val syncManager: SyncManager
) : ReciboRepository {

    override suspend fun generarYGuardarRecibo(reciboParcial: Recibo): AppResult<Recibo> {
        return safeApiCall {
            val idGenerado = reciboParcial.idRecibo.ifBlank { UUID.randomUUID().toString() }
            val online = networkMonitor.isCurrentlyOnline()

            val numeroGenerado = reciboParcial.numeroUnico.ifBlank {
                if (online) {
                    val recibosExistentes = runCatching { apiService.obtenerRecibos() }.getOrDefault(emptyList())
                    val maxNumero = recibosExistentes.mapNotNull { dto ->
                        dto.numeroUnico?.removePrefix("REC-")?.toIntOrNull()
                    }.maxOrNull() ?: 0
                    val siguiente = maxOf(recibosExistentes.size, maxNumero) + 1
                    "REC-${siguiente.toString().padStart(5, '0')}"
                } else {
                    val locales = reciboDao.obtenerTodos()
                    val maxLocal = locales.mapNotNull { entity ->
                        entity.numeroUnico.removePrefix("REC-").toIntOrNull()
                    }.maxOrNull() ?: 0
                    val siguiente = maxOf(locales.size, maxLocal) + 1
                    "REC-${siguiente.toString().padStart(5, '0')}"
                }
            }

            if (online) {
                val dtoAEnviar = ReciboDto(
                    idRecibo = null,
                    nombreInquilino = reciboParcial.nombreInquilino,
                    dniInquilino = reciboParcial.dniInquilino,
                    direccionInmueble = reciboParcial.direccionInmueble,
                    distritoInmueble = reciboParcial.distritoInmueble,
                    nombreUnidad = reciboParcial.nombreUnidad,
                    numeroUnico = numeroGenerado,
                    idContrato = reciboParcial.idContrato.ifBlank { null },
                    fechaEmision = reciboParcial.fechaEmision,
                    rentaBase = reciboParcial.rentaBase,
                    montoIgv = reciboParcial.montoIgv,
                    agua = reciboParcial.agua,
                    luz = reciboParcial.luz,
                    mantenimiento = reciboParcial.mantenimiento,
                    montoTotal = reciboParcial.montoTotal,
                    estadoPago = reciboParcial.estadoPago
                )

                val dtoCreado = apiService.generarRecibo(dtoAEnviar).first()
                val dominioCreado = dtoCreado.aDomain().copy(
                    idRecibo = dtoCreado.idRecibo?.takeIf { it.isNotBlank() } ?: idGenerado,
                    numeroUnico = dtoCreado.numeroUnico?.takeIf { it.isNotBlank() } ?: numeroGenerado
                )
                reciboDao.upsert(dominioCreado.toEntity(syncStatus = "SYNCED"))
                dominioCreado
            } else {
                val reciboOffline = reciboParcial.copy(
                    idRecibo = idGenerado,
                    numeroUnico = numeroGenerado
                )
                val entityOffline = reciboOffline.toEntity(syncStatus = "PENDING_CREATE")
                reciboDao.upsert(entityOffline)
                syncManager.encolarOperacion(
                    entityType = "RECIBO",
                    operationType = "CREATE",
                    entityId = idGenerado,
                    payload = entityOffline
                )
                reciboOffline
            }
        }
    }

    override suspend fun obtenerTodosLosRecibos(): AppResult<List<Recibo>> {
        return safeApiCall {
            if (networkMonitor.isCurrentlyOnline()) {
                runCatching { syncManager.sincronizarTodo() }
                val remotos = apiService.obtenerRecibos()
                val entidades = remotos.mapIndexedNotNull { idx, dto ->
                    val id = dto.idRecibo?.takeIf { it.isNotBlank() }
                        ?: dto.numeroUnico?.takeIf { it.isNotBlank() }
                        ?: "recibo_remoto_$idx"
                    dto.aDomain().copy(idRecibo = id).toEntity(syncStatus = "SYNCED")
                }
                reciboDao.upsertAll(entidades)
                val idsRemotos = entidades.map { it.idRecibo }
                if (idsRemotos.isNotEmpty()) {
                    reciboDao.eliminarObsoletosSincronizados(idsRemotos)
                }
            }
            reciboDao.obtenerTodos().map { it.toDomain() }
        }
    }

    override suspend fun actualizarEstadoPago(
        idRecibo: String,
        nuevoEstado: String
    ): AppResult<Unit> {
        if (nuevoEstado != Recibo.ESTADO_PENDIENTE && nuevoEstado != Recibo.ESTADO_COBRADO) {
            return AppResult.Error(IllegalArgumentException("El estado del recibo no es válido."))
        }

        if (idRecibo.isBlank()) {
            return AppResult.Error(Exception("El ID del recibo es obligatorio."))
        }

        return safeApiCall {
            if (networkMonitor.isCurrentlyOnline()) {
                apiService.actualizarEstadoPago(
                    idFiltro = "eq.$idRecibo",
                    body = ActualizarEstadoRequest(nuevoEstado)
                )
                val actual = reciboDao.obtenerPorId(idRecibo)
                if (actual != null) {
                    reciboDao.upsert(actual.copy(estadoPago = nuevoEstado, syncStatus = "SYNCED"))
                }
            } else {
                val actual = reciboDao.obtenerPorId(idRecibo)
                    ?: throw IllegalStateException("No se encontró el recibo local para actualizar offline.")
                val actualizadoEntity = actual.copy(
                    estadoPago = nuevoEstado,
                    syncStatus = "PENDING_UPDATE"
                )
                reciboDao.upsert(actualizadoEntity)
                syncManager.encolarOperacion(
                    entityType = "RECIBO",
                    operationType = "UPDATE",
                    entityId = idRecibo,
                    payload = actualizadoEntity
                )
            }
        }
    }

    private fun ReciboDto.aDomain(): Recibo {
        return Recibo(
            idRecibo = idRecibo ?: "",
            nombreInquilino = nombreInquilino,
            dniInquilino = dniInquilino,
            direccionInmueble = direccionInmueble,
            distritoInmueble = distritoInmueble,
            nombreUnidad = nombreUnidad,
            numeroUnico = numeroUnico ?: "",
            idContrato = idContrato ?: "",
            fechaEmision = fechaEmision,
            rentaBase = rentaBase,
            montoIgv = montoIgv,
            agua = agua,
            luz = luz,
            mantenimiento = mantenimiento,
            montoTotal = montoTotal,
            estadoPago = estadoPago
        )
    }

    private fun ReciboEntity.toDomain(): Recibo {
        return Recibo(
            idRecibo = idRecibo,
            nombreInquilino = nombreInquilino,
            dniInquilino = dniInquilino,
            direccionInmueble = direccionInmueble,
            distritoInmueble = distritoInmueble,
            nombreUnidad = nombreUnidad,
            numeroUnico = numeroUnico,
            idContrato = idContrato,
            fechaEmision = fechaEmision,
            rentaBase = rentaBase,
            montoIgv = montoIgv,
            agua = agua,
            luz = luz,
            mantenimiento = mantenimiento,
            montoTotal = montoTotal,
            estadoPago = estadoPago
        )
    }

    private fun Recibo.toEntity(syncStatus: String): ReciboEntity {
        return ReciboEntity(
            idRecibo = idRecibo,
            nombreInquilino = nombreInquilino,
            dniInquilino = dniInquilino,
            direccionInmueble = direccionInmueble,
            distritoInmueble = distritoInmueble,
            nombreUnidad = nombreUnidad,
            numeroUnico = numeroUnico,
            idContrato = idContrato,
            fechaEmision = fechaEmision,
            rentaBase = rentaBase,
            montoIgv = montoIgv,
            agua = agua,
            luz = luz,
            mantenimiento = mantenimiento,
            montoTotal = montoTotal,
            estadoPago = estadoPago,
            syncStatus = syncStatus
        )
    }
}