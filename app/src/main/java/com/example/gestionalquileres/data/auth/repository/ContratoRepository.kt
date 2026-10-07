package com.example.gestionalquileres.data.repository

import com.example.gestionalquileres.domain.model.Contrato
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ContratoRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val contratosCollection = firestore.collection("contratos")

    // Registra un nuevo contrato usando una Transacción para asegurar la consistencia total
    suspend fun registrarContrato(contrato: Contrato): Result<Contrato> {
        return try {
            val documentoRef = contratosCollection.document()
            val contratoConId = contrato.copy(
                idContrato = documentoRef.id,
                active = true
            )

            val unidadRef = firestore.collection("inmuebles")
                .document(contrato.inmuebleId)
                .collection("unidadesAlquilables")
                .document(contrato.unidadId)

            firestore.runTransaction { transaction ->
                val unidadSnapshot = transaction.get(unidadRef)
                val estaOcupada = unidadSnapshot.getBoolean("estaOcupada") ?: false

                if (estaOcupada) {
                    throw IllegalStateException("Esta unidad ya ha sido ocupada por otro contrato.")
                }

                transaction.set(documentoRef, contratoConId)
                transaction.update(unidadRef, "estaOcupada", true)
            }.await()

            Result.success(contratoConId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Obtiene todos los contratos activos (vigentes)
    suspend fun obtenerContratosActivos(): Result<List<Contrato>> {
        return try {
            val snapshot = contratosCollection
                .whereEqualTo("active", true)
                .get()
                .await()

            val contratos = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Contrato::class.java)
            }

            Result.success(contratos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // NUEVO: Obtiene el contrato activo de un inquilino específico (para validar si ya alquila algo)
    suspend fun obtenerContratoActivoPorInquilino(inquilinoId: String): Result<Contrato?> {
        return try {
            val snapshot = contratosCollection
                .whereEqualTo("inquilinoId", inquilinoId)
                .whereEqualTo("active", true)
                .limit(1)
                .get()
                .await()

            val contrato = snapshot.documents.firstOrNull()?.toObject(Contrato::class.java)
            Result.success(contrato)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // NUEVO: Obtiene el historial completo de contratos (tanto activos como inactivos/finalizados)
    suspend fun obtenerHistorialCompleto(): Result<List<Contrato>> {
        return try {
            val snapshot = contratosCollection
                .get()
                .await()

            val contratos = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Contrato::class.java)
            }

            Result.success(contratos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Finaliza un contrato y libera la unidad usando transacción
    suspend fun finalizarContrato(idContrato: String, inmuebleId: String, unidadId: String): Result<Unit> {
        return try {
            if (idContrato.isBlank() || inmuebleId.isBlank() || unidadId.isBlank()) {
                return Result.failure(Exception("Los identificadores son obligatorios para finalizar el contrato."))
            }

            val contratoRef = contratosCollection.document(idContrato)
            val unidadRef = firestore.collection("inmuebles")
                .document(inmuebleId)
                .collection("unidadesAlquilables")
                .document(unidadId)

            firestore.runTransaction { transaction ->
                transaction.update(contratoRef, "active", false)
                transaction.update(unidadRef, "estaOcupada", false)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}