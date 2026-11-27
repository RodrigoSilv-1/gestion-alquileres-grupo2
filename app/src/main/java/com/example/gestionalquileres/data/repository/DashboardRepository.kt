package com.example.gestionalquileres.data.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.CobranzaMes
import com.example.gestionalquileres.domain.model.Deudor
import com.example.gestionalquileres.domain.model.EstadoPago
import com.example.gestionalquileres.domain.model.Inmueble
import com.example.gestionalquileres.domain.model.Recibo
import com.example.gestionalquileres.domain.model.ResumenGeneral
import com.example.gestionalquileres.domain.redondearMonto
import com.example.gestionalquileres.domain.repository.DashboardRepository
import com.example.gestionalquileres.domain.sumarMontos
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/** Tope de recibos pendientes que se leen para armar la lista de mayores deudores. */
private const val MAX_PENDIENTES_LEIDOS = 500
private const val TOP_DEUDORES = 5

class DashboardRepositoryImpl @Inject constructor(
    private val contratoRepository: ContratoRepositoryImpl,
    private val inmuebleRepository: InmuebleRepositoryImpl,
    private val reciboRepository: ReciboRepositoryImpl
) : DashboardRepository {

    /** "Cobrado" es el valor antiguo que EstadoPago.desde() ya trata como pagado. */
    private val estadosPagado = listOf(EstadoPago.PAGADO.valor, "Cobrado")

    private fun List<Recibo>.entre(desde: String, hasta: String): List<Recibo> =
        filter { recibo ->
            val fecha = recibo.fechaEmision
            fecha.isNotBlank() && fecha >= desde && fecha <= hasta
        }

    private fun List<Recibo>.contarYSumar(): Pair<Int, Double> =
        size to sumarMontos(*map { it.montoTotal }.toDoubleArray()).redondearMonto()

    private suspend fun todosLosRecibos(): List<Recibo> =
        when (val res = reciboRepository.obtenerTodosLosRecibos()) {
            is AppResult.Exito -> res.valor
            is AppResult.Error -> throw res.excepcion
        }

    override suspend fun obtenerResumenGeneral(
        fechaInicio: String,
        fechaFin: String
    ): AppResult<ResumenGeneral> = appResultOf {
        coroutineScope {
            val recibosDef = async { todosLosRecibos() }
            val vigentesDef = async {
                when (val res = contratoRepository.obtenerContratosActivos()) {
                    is AppResult.Exito -> res.valor.count { it.active }
                    is AppResult.Error -> throw res.excepcion
                }
            }
            val unidadesDef = async { totalUnidades() }

            val recibos = recibosDef.await()
            val (cantidadPendientes, montoPendiente) = recibos
                .filter { it.estadoPago == EstadoPago.PENDIENTE.valor }
                .contarYSumar()

            val cobradoMes = recibos
                .filter { it.estadoPago in estadosPagado }
                .entre(fechaInicio, fechaFin)
                .contarYSumar()
                .second

            ResumenGeneral(
                unidadesTotales = unidadesDef.await(),
                contratosVigentes = vigentesDef.await(),
                recibosPendientes = cantidadPendientes,
                montoPendiente = montoPendiente,
                cobradoDelMes = cobradoMes
            )
        }
    }

    override suspend fun obtenerCobranzaMes(
        fechaInicio: String,
        fechaFin: String
    ): AppResult<CobranzaMes> = appResultOf {
        coroutineScope {
            val recibos = todosLosRecibos()
            val recibosMes = recibos.entre(fechaInicio, fechaFin)

            val (cantidadPagados, montoCobrado) = recibosMes
                .filter { it.estadoPago in estadosPagado }
                .contarYSumar()

            val (cantidadPendientes, montoPorCobrar) = recibosMes
                .filter { it.estadoPago == EstadoPago.PENDIENTE.valor }
                .contarYSumar()

            CobranzaMes(
                recibosPagados = cantidadPagados,
                recibosPendientes = cantidadPendientes,
                montoCobrado = montoCobrado,
                montoPorCobrar = montoPorCobrar,
                mayoresDeudores = mayoresDeudores(recibos)
            )
        }
    }

    /** Pisos × unidades por piso de todos los inmuebles activos. */
    private suspend fun totalUnidades(): Int {
        val inmuebles: List<Inmueble> = when (val res = inmuebleRepository.obtenerTodos()) {
            is AppResult.Exito -> res.valor
            is AppResult.Error -> throw res.excepcion
        }
        return inmuebles
            .filter { it.active }
            .sumOf { it.numeroPisos * it.unidadesPorPiso }
    }

    /** Agrupa los recibos pendientes por contrato y devuelve los de mayor deuda. */
    private fun mayoresDeudores(recibos: List<Recibo>): List<Deudor> =
        recibos
            .filter { it.estadoPago == EstadoPago.PENDIENTE.valor }
            .take(MAX_PENDIENTES_LEIDOS)
            .groupBy { it.idContrato }
            .map { (idContrato, lista) ->
                Deudor(
                    idContrato = idContrato,
                    nombreInquilino = lista.firstOrNull { it.nombreInquilino.isNotBlank() }
                        ?.nombreInquilino ?: "Inquilino sin registrar",
                    nombreUnidad = lista.firstOrNull { it.nombreUnidad.isNotBlank() }
                        ?.nombreUnidad.orEmpty(),
                    recibosPendientes = lista.size,
                    montoAdeudado = sumarMontos(*lista.map { it.montoTotal }.toDoubleArray()).redondearMonto()
                )
            }
            .sortedByDescending { it.montoAdeudado }
            .take(TOP_DEUDORES)
}