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
            // 1. Creamos de antemano un espacio con un ID único en la base de datos para el nuevo recibo
            val nuevoReciboRef = recibosCollection.document()

            // 2. Declaramos una variable temporal vacía que luego guardará el recibo completo y listo
            var reciboFinal: Recibo? = null

            // 3. Iniciamos una transacción: un bloque "todo o nada". Esto significa que si algo falla,
            // no se guarda nada, evitando que los números de los recibos se desordenen o se repitan.
            firestore.runTransaction { transaction ->

                // Paso A: Leemos el documento contador para saber cuál fue el último número de recibo que se generó
                val snapshotContador = transaction.get(contadorRef)
                val ultimoNumero = if (snapshotContador.exists()) {
                    // Si el contador ya existe, extraemos el número y lo convertimos a número entero; si falla, usamos 0
                    snapshotContador.getLong("ultimo")?.toInt() ?: 0
                } else {
                    // Si el contador no existe todavía (es el primer recibo del sistema), empezamos en 0
                    0
                }

                // Paso B: Calculamos el siguiente número sumándole 1 y lo formateamos con ceros a la izquierda (Ej: REC-00001)
                val nuevoNumero = ultimoNumero + 1
                val numeroFormateado = "REC-" + nuevoNumero.toString().padStart(5, '0')

                // Paso C: Completamos los datos faltantes del recibo (su ID único de base de datos y su código correlativo)
                reciboFinal = reciboParcial.copy(
                    idRecibo = nuevoReciboRef.id,
                    numeroUnico = numeroFormateado
                )

                // Paso D: Actualizamos el contador en la base de datos con el nuevo número que acabamos de usar...
                transaction.set(contadorRef, mapOf("ultimo" to nuevoNumero))

                // y al mismo tiempo guardamos el nuevo recibo completo en su respectiva colección
                transaction.set(nuevoReciboRef, reciboFinal!!)

            }.await() // Esperamos de forma asíncrona a que toda esta operación "todo o nada" termine de guardarse en la nube

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
    suspend fun actualizarEstadoPago(
        idRecibo: String,
        nuevoEstado: String
    ): AppResult<Unit> {
        if (
            nuevoEstado != Recibo.ESTADO_PENDIENTE &&
            nuevoEstado != Recibo.ESTADO_COBRADO
        ) {
            return AppResult.Error(
                IllegalArgumentException("El estado del recibo no es válido.")
            )
        }

        return try {
            recibosCollection
                .document(idRecibo)
                .update("estadoPago", nuevoEstado)
                .await()

            AppResult.Exito(Unit)
        } catch (e: Exception) {
            AppResult.Error(e)
        }
    }
}