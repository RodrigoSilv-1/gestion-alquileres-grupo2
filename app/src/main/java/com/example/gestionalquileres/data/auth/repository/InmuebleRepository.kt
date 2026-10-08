package com.example.gestionalquileres.data.auth.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Inmueble
import com.example.gestionalquileres.domain.model.Inquilino
import com.example.gestionalquileres.domain.model.UnidadAlquilable
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class InmuebleRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    // Referencia a la coleccion principal en Firestore
    private val collection = firestore.collection("inmuebles")

    suspend fun registrar(inmueble: Inmueble): AppResult<Inmueble> {
        return try {
            val documento = collection.document()
            val idInmueble = documento.id // Corregido: se quitó la coma

            val nuevoInmueble = inmueble.copy(
                idInmueble = idInmueble, // Corregido: se usa la variable correcta
                active = true
            )

            // Generamos automáticamente las unidades alquilables usando la subcolección ANTES de retornar
            generarUnidadesAlquilables(documento, idInmueble, inmueble.numeroPisos, inmueble.unidadesPorPiso)

            documento.set(nuevoInmueble).await()

            AppResult.Exito(nuevoInmueble) // Todo usa AppResult
        } catch (error: Exception) {
            AppResult.Error(error)
        }
    }

    private suspend fun generarUnidadesAlquilables(
        inmuebleRef: com.google.firebase.firestore.DocumentReference,
        inmuebleId: String,
        numeroPisos: Int,
        unidadesPorPiso: Int
    ) {
        val unidadesCollection = inmuebleRef.collection("unidadesAlquilables")

        for (piso in 1..numeroPisos) {
            for (unidadNum in 1..unidadesPorPiso) {
                val unidadRef = unidadesCollection.document()

                // Nomenclatura: "101"
                val nombreFormateado = "$piso${unidadNum.toString().padStart(2, '0')}"

                val unidadAlquilable = UnidadAlquilable(
                    id = unidadRef.id,
                    inmuebleId = inmuebleId,
                    piso = piso,
                    numeroUnidad = unidadNum,
                    nombreFormateado = nombreFormateado,
                    estaOcupada = false
                )

                unidadRef.set(unidadAlquilable).await()
            }
        }
    }


    // Trae solo los inmuebles que están activos
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

    // Filtra inmuebles activos por coincidencia exacta del código postal
    suspend fun obtenerPorCodigoPostal(codigoPostal: String): AppResult<List<Inmueble>> {
        return try {
            val snapshot = collection
                .whereEqualTo("codigoPostal", codigoPostal)
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

    // Busca inmuebles activos por coincidencia de texto en el distrito
    suspend fun obtenerporDistrito(distrito: String): Result<List<Inmueble>> {
        return try {
            val snapshot = collection
                .whereEqualTo("active", true)
                .get()
                .await()

            val inmuebles = snapshot.documents
                .mapNotNull { documento ->
                    documento.toObject(Inmueble::class.java)
                }
                .filter { inmueble ->
                    inmueble.distrito.contains(distrito, ignoreCase = true)
                }

            Result.success(inmuebles)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    // Sobrescribe los datos de un inmueble existente usando su ID
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