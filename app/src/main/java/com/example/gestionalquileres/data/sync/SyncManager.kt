package com.example.gestionalquileres.data.sync

import com.example.gestionalquileres.data.local.dao.InmuebleDao
import com.example.gestionalquileres.data.local.dao.InquilinoDao
import com.example.gestionalquileres.data.local.dao.PendingOperationDao
import com.example.gestionalquileres.data.local.dao.ReciboDao
import com.example.gestionalquileres.data.local.entity.InmuebleEntity
import com.example.gestionalquileres.data.local.entity.InquilinoEntity
import com.example.gestionalquileres.data.local.entity.PendingOperationEntity
import com.example.gestionalquileres.data.local.entity.ReciboEntity
import com.example.gestionalquileres.data.network.NetworkMonitor
import com.example.gestionalquileres.data.remote.ActualizarEstadoRequest
import com.example.gestionalquileres.data.remote.ApiService
import com.example.gestionalquileres.data.remote.InmuebleDto
import com.example.gestionalquileres.data.remote.InquilinoDto
import com.example.gestionalquileres.data.remote.ReciboDto
import com.example.gestionalquileres.data.remote.UnidadAlquilableDto
import com.google.gson.Gson
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val pendingOperationDao: PendingOperationDao,
    private val inmuebleDao: InmuebleDao,
    private val inquilinoDao: InquilinoDao,
    private val reciboDao: ReciboDao,
    private val apiService: ApiService,
    private val networkMonitor: NetworkMonitor,
    private val gson: Gson
) {
    private val mutex = Mutex()

    suspend fun encolarOperacion(
        entityType: String,
        operationType: String,
        entityId: String,
        payload: Any
    ) {
        pendingOperationDao.encolar(
            PendingOperationEntity(
                entityType = entityType,
                operationType = operationType,
                entityId = entityId,
                payloadJson = gson.toJson(payload)
            )
        )
    }

    suspend fun sincronizarTodo(): Result<Unit> = mutex.withLock {
        if (!networkMonitor.isCurrentlyOnline()) {
            return Result.failure(IllegalStateException("Sin conexión a internet"))
        }

        return runCatching {
            val pendientes = pendingOperationDao.obtenerPendientesOrdenados()

            for (op in pendientes) {
                when (op.entityType) {
                    "INMUEBLE" -> procesarInmueble(op)
                    "INQUILINO" -> procesarInquilino(op)
                    "RECIBO" -> procesarRecibo(op)
                }
                pendingOperationDao.eliminar(op)
            }

            conciliarInmuebles()
            conciliarInquilinos()
            conciliarRecibos()
        }
    }

    private suspend fun procesarInmueble(op: PendingOperationEntity) {
        val dto = gson.fromJson(op.payloadJson, InmuebleDto::class.java)
        when (op.operationType) {
            "CREATE" -> {
                val idTemporal = dto.id
                val creado = apiService.registrarInmueble(dto).firstOrNull() ?: dto
                val idFinal = creado.id?.takeIf { it.isNotBlank() }
                    ?: idTemporal?.takeIf { it.isNotBlank() }
                    ?: return

                val pisos = creado.numeroPisos.takeIf { it > 0 } ?: dto.numeroPisos
                val unidadesPorPiso = creado.unidadesPorPiso.takeIf { it > 0 } ?: dto.unidadesPorPiso

                if (pisos > 0 && unidadesPorPiso > 0) {
                    for (piso in 1..pisos) {
                        for (unidad in 1..unidadesPorPiso) {
                            val numUnidad = (piso * 100) + unidad
                            val nuevaUnidadDto = UnidadAlquilableDto(
                                id = null,
                                inmuebleId = idFinal,
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

                inmuebleDao.upsert(creado.toEntity(idFinal, "SYNCED"))
                if (!idTemporal.isNullOrBlank() && idFinal != idTemporal) {
                    runCatching { inmuebleDao.eliminarPorId(idTemporal) }
                }
            }
            "UPDATE" -> {
                val id = dto.id?.takeIf { it.isNotBlank() } ?: op.entityId
                val actualizado = apiService.actualizarInmueble("eq.$id", dto).firstOrNull() ?: dto
                inmuebleDao.upsert(actualizado.toEntity(id, "SYNCED"))
            }
        }
    }

    private suspend fun procesarInquilino(op: PendingOperationEntity) {
        val entity = gson.fromJson(op.payloadJson, InquilinoEntity::class.java)
        when (op.operationType) {
            "CREATE" -> {
                val idTemporal = entity.idInquilino
                val dtoAEnviar = InquilinoDto(
                    idInquilino = null,
                    nombre = entity.nombre,
                    dni = entity.dni,
                    telefono = entity.telefono,
                    edad = entity.edad,
                    sexo = entity.sexo,
                    fotografia = entity.fotografia,
                    active = entity.active,
                    sueldo = entity.sueldo
                )
                val creado = apiService.registrarInquilino(dtoAEnviar).firstOrNull() ?: dtoAEnviar
                val idFinal = creado.idInquilino?.takeIf { it.isNotBlank() } ?: idTemporal
                inquilinoDao.upsert(
                    entity.copy(
                        idInquilino = idFinal,
                        fotografia = creado.fotografia ?: entity.fotografia,
                        syncStatus = "SYNCED"
                    )
                )
                if (idTemporal.isNotBlank() && idFinal != idTemporal) {
                    inquilinoDao.eliminarPorId(idTemporal)
                }
            }
            "UPDATE" -> {
                val idObjetivo = entity.idInquilino.ifBlank { op.entityId }
                val dtoAEnviar = InquilinoDto(
                    idInquilino = idObjetivo,
                    nombre = entity.nombre,
                    dni = entity.dni,
                    telefono = entity.telefono,
                    edad = entity.edad,
                    sexo = entity.sexo,
                    fotografia = entity.fotografia,
                    active = entity.active,
                    sueldo = entity.sueldo
                )
                apiService.actualizarInquilino(
                    idFiltro = "eq.$idObjetivo",
                    inquilino = dtoAEnviar
                )
                inquilinoDao.upsert(entity.copy(idInquilino = idObjetivo, syncStatus = "SYNCED"))
            }
        }
    }

    private suspend fun procesarRecibo(op: PendingOperationEntity) {
        val entity = gson.fromJson(op.payloadJson, ReciboEntity::class.java)
        when (op.operationType) {
            "CREATE" -> {
                val idTemporal = entity.idRecibo
                val dtoAEnviar = ReciboDto(
                    idRecibo = null,
                    nombreInquilino = entity.nombreInquilino,
                    dniInquilino = entity.dniInquilino,
                    direccionInmueble = entity.direccionInmueble,
                    distritoInmueble = entity.distritoInmueble,
                    nombreUnidad = entity.nombreUnidad,
                    numeroUnico = entity.numeroUnico,
                    idContrato = entity.idContrato.ifBlank { null },
                    fechaEmision = entity.fechaEmision,
                    rentaBase = entity.rentaBase,
                    montoIgv = entity.montoIgv,
                    agua = entity.agua,
                    luz = entity.luz,
                    mantenimiento = entity.mantenimiento,
                    montoTotal = entity.montoTotal,
                    estadoPago = entity.estadoPago
                )
                val creado = apiService.generarRecibo(dtoAEnviar).firstOrNull() ?: dtoAEnviar
                val idFinal = creado.idRecibo?.takeIf { it.isNotBlank() } ?: idTemporal
                reciboDao.upsert(
                    entity.copy(
                        idRecibo = idFinal,
                        numeroUnico = creado.numeroUnico?.takeIf { it.isNotBlank() } ?: entity.numeroUnico,
                        syncStatus = "SYNCED"
                    )
                )
                if (idTemporal.isNotBlank() && idFinal != idTemporal) {
                    reciboDao.eliminarPorId(idTemporal)
                }
            }
            "UPDATE" -> {
                val idObjetivo = entity.idRecibo.ifBlank { op.entityId }
                apiService.actualizarEstadoPago(
                    idFiltro = "eq.$idObjetivo",
                    body = ActualizarEstadoRequest(entity.estadoPago)
                )
                reciboDao.upsert(entity.copy(idRecibo = idObjetivo, syncStatus = "SYNCED"))
            }
        }
    }

    private suspend fun conciliarInmuebles() {
        val remotos = apiService.obtenerInmuebles().filter { it.active }
        val entidades = remotos.mapNotNull { dto ->
            val id = dto.id ?: return@mapNotNull null
            dto.toEntity(id, "SYNCED")
        }
        inmuebleDao.upsertAll(entidades)
        val ids = entidades.map { it.id }
        if (ids.isNotEmpty()) {
            inmuebleDao.eliminarObsoletosSincronizados(ids)
        }
    }

    private suspend fun conciliarInquilinos() {
        val remotosActivos = apiService.obtenerInquilinos().filter { it.active }
        val entidades = remotosActivos.mapIndexedNotNull { idx, dto ->
            val id = dto.idInquilino?.takeIf { it.isNotBlank() } ?: "remoto_${dto.dni}_$idx"
            InquilinoEntity(
                idInquilino = id,
                nombre = dto.nombre,
                dni = dto.dni,
                telefono = dto.telefono,
                edad = dto.edad,
                sexo = dto.sexo,
                fotografia = dto.fotografia ?: "",
                active = dto.active,
                sueldo = dto.sueldo,
                syncStatus = "SYNCED"
            )
        }
        inquilinoDao.upsertAll(entidades)
        val ids = entidades.map { it.idInquilino }
        if (ids.isNotEmpty()) {
            inquilinoDao.eliminarObsoletosSincronizados(ids)
        }
    }

    private suspend fun conciliarRecibos() {
        val remotos = apiService.obtenerRecibos()
        val entidades = remotos.mapIndexedNotNull { idx, dto ->
            val id = dto.idRecibo?.takeIf { it.isNotBlank() }
                ?: dto.numeroUnico?.takeIf { it.isNotBlank() }
                ?: "recibo_remoto_$idx"
            ReciboEntity(
                idRecibo = id,
                nombreInquilino = dto.nombreInquilino,
                dniInquilino = dto.dniInquilino,
                direccionInmueble = dto.direccionInmueble,
                distritoInmueble = dto.distritoInmueble,
                nombreUnidad = dto.nombreUnidad,
                numeroUnico = dto.numeroUnico ?: "",
                idContrato = dto.idContrato ?: "",
                fechaEmision = dto.fechaEmision,
                rentaBase = dto.rentaBase,
                montoIgv = dto.montoIgv,
                agua = dto.agua,
                luz = dto.luz,
                mantenimiento = dto.mantenimiento,
                montoTotal = dto.montoTotal,
                estadoPago = dto.estadoPago,
                syncStatus = "SYNCED"
            )
        }
        reciboDao.upsertAll(entidades)
        val ids = entidades.map { it.idRecibo }
        if (ids.isNotEmpty()) {
            reciboDao.eliminarObsoletosSincronizados(ids)
        }
    }

    private fun InmuebleDto.toEntity(idFinal: String, status: String) = InmuebleEntity(
        id = idFinal,
        distrito = distrito,
        direccion = direccion,
        numeroDireccion = numeroDireccion,
        codigoPostal = codigoPostal,
        numeroPisos = numeroPisos,
        unidadesPorPiso = unidadesPorPiso,
        fotografia = fotografia,
        active = active,
        syncStatus = status
    )
}