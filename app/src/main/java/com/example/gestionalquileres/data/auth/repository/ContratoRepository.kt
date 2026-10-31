package com.example.gestionalquileres.data.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Contrato
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ContratoRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val contratosCollection = firestore.collection("contratos")

    suspend fun registrarContrato(contrato: Contrato): AppResult<Contrato> {
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

            AppResult.Exito(contratoConId)
        } catch (e: Exception) {
            AppResult.Error(e)
        }
    }

    suspend fun obtenerContratosActivos(): AppResult<List<Contrato>> {
        return try {
            val snapshot = contratosCollection
                .whereEqualTo("active", true)
                .get()
                .await()

            val contratos = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Contrato::class.java)
            }

            AppResult.Exito(contratos)
        } catch (e: Exception) {
            AppResult.Error(e)
        }
    }

    suspend fun obtenerContratoActivoPorInquilino(inquilinoId: String): AppResult<Contrato?> {
        return try {
            val snapshot = contratosCollection
                .whereEqualTo("inquilinoId", inquilinoId)
                .whereEqualTo("active", true)
                .limit(1)
                .get()
                .await()

            val contrato = snapshot.documents.firstOrNull()?.toObject(Contrato::class.java)
            AppResult.Exito(contrato)
        } catch (e: Exception) {
            AppResult.Error(e)
        }
    }

    suspend fun obtenerHistorialCompleto(): AppResult<List<Contrato>> {
        return try {
            val snapshot = contratosCollection
                .get()
                .await()

            val contratos = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Contrato::class.java)
            }

            AppResult.Exito(contratos)
        } catch (e: Exception) {
            AppResult.Error(e)
        }
    }

    suspend fun finalizarContrato(idContrato: String, inmuebleId: String, unidadId: String): AppResult<Unit> {
        return try {
            if (idContrato.isBlank() || inmuebleId.isBlank() || unidadId.isBlank()) {
                return AppResult.Error(Exception("Los identificadores son obligatorios para finalizar el contrato."))
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

            AppResult.Exito(Unit)
        } catch (e: Exception) {
            AppResult.Error(e)
        }
    }
}