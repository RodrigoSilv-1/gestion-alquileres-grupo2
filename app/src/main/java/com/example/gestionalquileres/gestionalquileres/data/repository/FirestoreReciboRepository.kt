package com.example.gestionalquileres.data.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.repository.ReciboRepository
import com.example.gestionalquileres.domain.model.Recibo
import com.google.firebase.firestore.FirebaseFirestore
import com.example.gestionalquileres.domain.model.CursorPagina
import com.example.gestionalquileres.domain.model.EstadoPago
import com.example.gestionalquileres.domain.model.FiltroRecibos
import com.example.gestionalquileres.domain.model.Pagina
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreReciboRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : ReciboRepository {
    private val recibosCollection = firestore.collection("recibos")
    private val contadorRef = firestore.collection("contadores").document("recibos")

    override suspend fun generarYGuardarRecibo(reciboParcial: Recibo): AppResult<Recibo> = appResultOf {
        val nuevoReciboRef = recibosCollection.document()

        firestore.runTransaction { transaction ->
            // 1. Leer el último número de recibo generado
            val snapshotContador = transaction.get(contadorRef)
            val ultimoNumero = if (snapshotContador.exists()) {
                snapshotContador.getLong("ultimo")?.toInt() ?: 0
            } else {
                0
            }

            // 2. Generar el nuevo correlativo (Ej: REC-00001)
            val nuevoNumero = ultimoNumero + 1
            val numeroFormateado = "REC-" + nuevoNumero.toString().padStart(5, '0')

            // 3. Completar el objeto Recibo con sus IDs
            val reciboFinal = reciboParcial.copy(
                idRecibo = nuevoReciboRef.id,
                numeroUnico = numeroFormateado
            )

            // 4. Guardar ambos cambios atómicamente
            transaction.set(contadorRef, mapOf("ultimo" to nuevoNumero))
            transaction.set(nuevoReciboRef, reciboFinal)

            reciboFinal // lo que devuelve la transacción
        }.await()
    }

    override suspend fun obtenerPagina(
        filtro: FiltroRecibos,
        limite: Int,
        despues: CursorPagina?
    ): AppResult<Pagina<Recibo>> = appResultOf {
        val numero = filtro.numero?.trim()?.uppercase().orEmpty()

        val consulta: Query = if (numero.isNotEmpty()) {
            // Búsqueda exacta por correlativo: como máximo 1 documento, sin orderBy (no necesita índice)
            recibosCollection.whereEqualTo("numeroUnico", numero)
        } else {
            var q: Query = recibosCollection
            if (!filtro.idContrato.isNullOrBlank()) q = q.whereEqualTo("idContrato", filtro.idContrato)
            if (filtro.estado != null) q = q.whereEqualTo("estadoPago", filtro.estado.valor)
            if (!filtro.desde.isNullOrBlank()) q = q.whereGreaterThanOrEqualTo("fechaEmision", filtro.desde)
            if (!filtro.hasta.isNullOrBlank()) q = q.whereLessThanOrEqualTo("fechaEmision", filtro.hasta)
            q.orderBy("fechaEmision", Query.Direction.DESCENDING)
        }

        consulta.leerPagina(limite, despues) { doc -> doc.toObject(Recibo::class.java) }
    }

    override suspend fun actualizarEstadoPago(
        idRecibo: String,
        nuevoEstado: EstadoPago
    ): AppResult<Unit> = appResultOf {
        recibosCollection
            .document(idRecibo)
            .update("estadoPago", nuevoEstado.valor)
            .await()
        Unit
    }
}