package com.example.gestionalquileres.data.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.CobranzaMes
import com.example.gestionalquileres.domain.model.Deudor
import com.example.gestionalquileres.domain.model.EstadoPago
import com.example.gestionalquileres.domain.model.Recibo
import com.example.gestionalquileres.domain.model.ResumenGeneral
import com.example.gestionalquileres.domain.redondearMonto
import com.example.gestionalquileres.domain.repository.DashboardRepository
import com.example.gestionalquileres.domain.sumarMontos
import com.google.firebase.firestore.AggregateField
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** Tope de recibos pendientes que se leen para armar la lista de mayores deudores. */
private const val MAX_PENDIENTES_LEIDOS = 500L
private const val TOP_DEUDORES = 5

class FirestoreDashboardRepository @Inject constructor(
    firestore: FirebaseFirestore
) : DashboardRepository {

    private val recibos = firestore.collection("recibos")
    private val contratos = firestore.collection("contratos")
    private val inmuebles = firestore.collection("inmuebles")

    /** "Cobrado" es el valor antiguo que EstadoPago.desde() ya trata como pagado. */
    private val estadosPagado = listOf(EstadoPago.PAGADO.valor, "Cobrado")

    private fun consultaPendientes(): Query =
        recibos.whereEqualTo("estadoPago", EstadoPago.PENDIENTE.valor)

    private fun Query.entre(desde: String, hasta: String): Query =
        whereGreaterThanOrEqualTo("fechaEmision", desde)
            .whereLessThanOrEqualTo("fechaEmision", hasta)

    /** Cuenta los recibos de la consulta y suma su montoTotal, ambos calculados en el servidor. */
    private suspend fun Query.contarYSumar(): Pair<Int, Double> {
        val cuenta = AggregateField.count()
        val suma = AggregateField.sum("montoTotal")
        val resultado = aggregate(cuenta, suma).get(AggregateSource.SERVER).await()
        return (resultado.getLong(cuenta) ?: 0L).toInt() to (resultado.getDouble(suma) ?: 0.0).redondearMonto()
    }

    override suspend fun obtenerResumenGeneral(
        desdeMes: String,
        hastaMes: String
    ): AppResult<ResumenGeneral> = appResultOf {
        coroutineScope {
            val pendientesDef = async { consultaPendientes().contarYSumar() }
            val cobradoDef = async {
                recibos.whereIn("estadoPago", estadosPagado).entre(desdeMes, hastaMes).contarYSumar()
            }
            val vigentesDef = async {
                contratos.whereEqualTo("active", true).count().get(AggregateSource.SERVER).await().count
            }
            val unidadesDef = async { totalUnidades() }

            val (cantidadPendientes, montoPendiente) = pendientesDef.await()
            ResumenGeneral(
                unidadesTotales = unidadesDef.await(),
                contratosVigentes = vigentesDef.await().toInt(),
                recibosPendientes = cantidadPendientes,
                montoPendiente = montoPendiente,
                cobradoDelMes = cobradoDef.await().second
            )
        }
    }

    override suspend fun obtenerCobranzaMes(
        desde: String,
        hasta: String
    ): AppResult<CobranzaMes> = appResultOf {
        coroutineScope {
            val pagadosDef = async {
                recibos.whereIn("estadoPago", estadosPagado).entre(desde, hasta).contarYSumar()
            }
            val pendientesDef = async { consultaPendientes().entre(desde, hasta).contarYSumar() }
            val deudoresDef = async { mayoresDeudores() }

            val (cantidadPagados, montoCobrado) = pagadosDef.await()
            val (cantidadPendientes, montoPorCobrar) = pendientesDef.await()
            CobranzaMes(
                recibosPagados = cantidadPagados,
                recibosPendientes = cantidadPendientes,
                montoCobrado = montoCobrado,
                montoPorCobrar = montoPorCobrar,
                mayoresDeudores = deudoresDef.await()
            )
        }
    }

    /** Pisos × unidades por piso de todos los inmuebles activos. */
    private suspend fun totalUnidades(): Int =
        inmuebles.whereEqualTo("active", true).get().await().documents.sumOf { doc ->
            (doc.getLong("numeroPisos") ?: 0L) * (doc.getLong("unidadesPorPiso") ?: 0L)
        }.toInt()

    /** Agrupa los recibos pendientes por contrato y devuelve los de mayor deuda. */
    private suspend fun mayoresDeudores(): List<Deudor> =
        consultaPendientes()
            .limit(MAX_PENDIENTES_LEIDOS)
            .get().await().documents
            .mapNotNull { it.toObject(Recibo::class.java) }
            .groupBy { it.idContrato }
            .map { (idContrato, lista) ->
                Deudor(
                    idContrato = idContrato,
                    // Los recibos antiguos no guardan inquilino ni unidad
                    nombreInquilino = lista.firstOrNull { it.nombreInquilino.isNotBlank() }
                        ?.nombreInquilino ?: "Inquilino sin registrar",
                    nombreUnidad = lista.firstOrNull { it.nombreUnidad.isNotBlank() }?.nombreUnidad.orEmpty(),
                    recibosPendientes = lista.size,
                    montoAdeudado = sumarMontos(*lista.map { it.montoTotal }.toDoubleArray())
                )
            }
            .sortedByDescending { it.montoAdeudado }
            .take(TOP_DEUDORES)
}