package com.example.gestionalquileres.data.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Inmueble
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class InmuebleRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val collection = firestore.collection("inmuebles")

    suspend fun registrar(inmueble: Inmueble): AppResult<Inmueble> {
        return try {
            val documento = collection.document()
            val nuevoInmueble = inmueble.copy(
                idInmueble = documento.id,
                active = true
            )
            documento.set(nuevoInmueble).await()
            AppResult.Exito(nuevoInmueble)
        } catch (error: Exception) {
            AppResult.Error(error)
        }
    }

    suspend fun obtenerTodos(): AppResult<List<Inmueble>> {
        return try {
            val snapshot = collection
                .whereEqualTo("active", true)
                .get()
                .await()

            val inmuebles = snapshot.documents.mapNotNull { documento ->
                documento.toObject(Inmueble::class.java)
            }
            AppResult.Exito(inmuebles)
        } catch (error: Exception) {
            AppResult.Error(error)
        }
    }

    suspend fun actualizar(inmueble: Inmueble): AppResult<Inmueble> {
        return try {
            if (inmueble.idInmueble.isBlank()) {
                return AppResult.Error(Exception("El ID del inmueble es obligatorio."))
            }

            collection
                .document(inmueble.idInmueble)
                .set(inmueble)
                .await()

            AppResult.Exito(inmueble)
        } catch (error: Exception) {
            AppResult.Error(error)
        }
    }

    suspend fun darDeBaja(idInmueble: String): AppResult<Unit> {
        return try {
            if (idInmueble.isBlank()) {
                return AppResult.Error(Exception("El ID del inmueble es obligatorio."))
            }

            collection
                .document(idInmueble)
                .update("active", false)
                .await()

            AppResult.Exito(Unit)
        } catch (error: Exception) {
            AppResult.Error(error)
        }
    }

    suspend fun reactivarYActualizar(inmueble: Inmueble): AppResult<Inmueble> {
        return try {
            val inmuebleReactivado = inmueble.copy(active = true)
            collection
                .document(inmuebleReactivado.idInmueble)
                .set(inmuebleReactivado)
                .await()

            AppResult.Exito(inmuebleReactivado)
        } catch (error: Exception) {
            AppResult.Error(error)
        }
    }
}