package com.example.gestionalquileres.data.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Recibo
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ReciboRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val recibosCollection = firestore.collection("recibos")
    private val contadorRef = firestore.collection("contadores").document("recibos")

    suspend fun generarYGuardarRecibo(reciboParcial: Recibo): AppResult<Recibo> {
        return try {
            val nuevoReciboRef = recibosCollection.document()
            var reciboFinal: Recibo? = null

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
                reciboFinal = reciboParcial.copy(
                    idRecibo = nuevoReciboRef.id,
                    numeroUnico = numeroFormateado
                )

                // 4. Guardar ambos cambios atómicamente (Criterio 2)
                transaction.set(contadorRef, mapOf("ultimo" to nuevoNumero))
                transaction.set(nuevoReciboRef, reciboFinal!!)
            }.await()

            if (reciboFinal != null) {
                AppResult.Exito(reciboFinal!!)
            } else {
                AppResult.Error(Exception("Error al generar el documento de recibo."))
            }
        } catch (e: Exception) {
            AppResult.Error(e) // OJO: Usando AppResult para evitar el bug de Hilt/KSP
        }
    }

    // Trae el historial de recibos generados
    suspend fun obtenerTodosLosRecibos(): AppResult<List<Recibo>> {
        return try {
            val snapshot = recibosCollection.get().await()
            val listaRecibos = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Recibo::class.java)
            }
            AppResult.Exito(listaRecibos)
        } catch (e: Exception) {
            AppResult.Error(e)
        }
    }
}