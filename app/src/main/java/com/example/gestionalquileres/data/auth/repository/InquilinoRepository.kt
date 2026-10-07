package com.example.gestionalquileres.data.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Inquilino
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class InquilinoRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val collection = firestore.collection("inquilinos")

    suspend fun registrar(inquilino: Inquilino): AppResult<Inquilino> {
        return try {
            val documento = collection.document()

            val nuevoInquilino = inquilino.copy(
                idInquilino = documento.id,
                active = true
            )

            documento.set(nuevoInquilino).await()

            AppResult.Exito(nuevoInquilino)
        } catch (error: Exception) {
            AppResult.Error(error)
        }
    }

    suspend fun obtenerTodos(): AppResult<List<Inquilino>> {
        return try {
            val snapshot = collection
                .whereEqualTo("active", true)
                .get()
                .await()

            val inquilinos = snapshot.documents.mapNotNull { documento ->
                documento.toObject(Inquilino::class.java)
            }

            AppResult.Exito(inquilinos)
        } catch (error: Exception) {
            AppResult.Error(error)
        }
    }

    suspend fun obtenerPorDni(dni: String): AppResult<List<Inquilino>> {
        return try {
            val snapshot = collection
                .whereEqualTo("dni", dni)
                .whereEqualTo("active", true)
                .get()
                .await()

            val inquilinos = snapshot.documents.mapNotNull { documento ->
                documento.toObject(Inquilino::class.java)
            }

            AppResult.Exito(inquilinos)
        } catch (error: Exception) {
            AppResult.Error(error)
        }
    }

    suspend fun obtenerPorNombre(nombre: String): AppResult<List<Inquilino>> {
        return try {
            val snapshot = collection
                .whereEqualTo("active", true)
                .get()
                .await()

            val inquilinos = snapshot.documents
                .mapNotNull { documento ->
                    documento.toObject(Inquilino::class.java)
                }
                .filter { inquilino ->
                    inquilino.nombre.contains(nombre, ignoreCase = true)
                }

            AppResult.Exito(inquilinos)
        } catch (error: Exception) {
            AppResult.Error(error)
        }
    }

    suspend fun actualizar(inquilino: Inquilino): AppResult<Inquilino> {
        return try {
            if (inquilino.idInquilino.isBlank()) {
                return AppResult.Error(
                    Exception("El ID del inquilino es obligatorio.")
                )
            }

            collection
                .document(inquilino.idInquilino)
                .set(inquilino)
                .await()

            AppResult.Exito(inquilino)
        } catch (error: Exception) {
            AppResult.Error(error)
        }
    }

    suspend fun darDeBaja(idInquilino: String): AppResult<Unit> {
        return try {
            if (idInquilino.isBlank()) {
                return AppResult.Error(
                    Exception("El ID del inquilino es obligatorio.")
                )
            }

            collection
                .document(idInquilino)
                .update("active", false)
                .await()

            AppResult.Exito(Unit)
        } catch (error: Exception) {
            AppResult.Error(error)
        }
    }

    suspend fun buscarPorDniCualquiera(dni: String): AppResult<Inquilino?> {
        return try {
            val snapshot = collection
                .whereEqualTo("dni", dni)
                .limit(1)
                .get()
                .await()

            val inquilino = snapshot.documents.firstOrNull()?.toObject(Inquilino::class.java)
            AppResult.Exito(inquilino)
        } catch (error: Exception) {
            AppResult.Error(error)
        }
    }

    suspend fun reactivarYActualizar(inquilino: Inquilino): AppResult<Inquilino> {
        return try {
            val inquilinoReactivado = inquilino.copy(active = true)
            collection
                .document(inquilinoReactivado.idInquilino)
                .set(inquilinoReactivado)
                .await()

            AppResult.Exito(inquilinoReactivado)
        } catch (error: Exception) {
            AppResult.Error(error)
        }
    }
}