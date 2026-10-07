package com.example.gestionalquileres.data.auth.repository

import com.example.gestionalquileres.domain.model.Inmueble
import com.example.gestionalquileres.domain.model.Inquilino
import com.example.gestionalquileres.domain.model.UnidadAlquilable
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class InmuebleRepository (
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // Referencia a la coleccion principal en Firestore
    private val collection = firestore.collection("inmuebles")

    // Guarda un nuevo inmueble generando un ID único en Firestore
    suspend fun registrar(inmueble: Inmueble): Result<Inmueble> {
        return try {
            val documento = collection.document()
            val inmuebleId = documento.id

            val nuevoInmueble = inmueble.copy(
                idInmueble = inmuebleId,
                active = true
            )

            documento.set(nuevoInmueble).await()

            // Generamos automáticamente las unidades alquilables usando la subcolección
            generarUnidadesAlquilables(documento, inmuebleId, inmueble.numeroPisos, inmueble.unidadesPorPiso)

            Result.success(nuevoInmueble)
        } catch (error: Exception) {
            Result.failure(error)
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
    suspend fun obtenerTodos(): Result<List<Inmueble>> {
        return try {
            val snapshot = collection
                .whereEqualTo("active", true)
                .get()
                .await()

            val inmuebles = snapshot.documents.mapNotNull { documento ->
                documento.toObject(Inmueble::class.java)
            }

            Result.success(inmuebles)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    // Filtra inmuebles activos por coincidencia exacta del código postal
    suspend fun obtenerPorCodigoPostal(codigoPostal: String): Result<List<Inmueble>> {
        return try {
            val snapshot = collection
                .whereEqualTo("codigoPostal", codigoPostal)
                .whereEqualTo("active", true)
                .get()
                .await()

            val inmuebles = snapshot.documents.mapNotNull { documento ->
                documento.toObject(Inmueble::class.java)
            }

            Result.success(inmuebles)
        } catch (error: Exception) {
            Result.failure(error)
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
    suspend fun actualizar(inmueble: Inmueble): Result<Inmueble> {
        return try {
            if (inmueble.idInmueble.isBlank()) {
                return Result.failure(
                    Exception("El ID del inmueble es obligatorio.")
                )
            }

            collection
                .document(inmueble.idInmueble)
                .set(inmueble)
                .await()

            Result.success(inmueble)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    // Baja lógica: no borra el documento, solo marca active = false
    suspend fun darDeBaja(idInmueble: String): Result<Unit> {
        return try {
            if (idInmueble.isBlank()) {
                return Result.failure(
                    Exception("El ID del inmueble es obligatorio.")
                )
            }

            collection
                .document(idInmueble)
                .update("active", false)
                .await()

            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    // Vuelve a activar un inmueble dado de baja y actualiza su información
    suspend fun reactivarYActualizar(inmueble: Inmueble): Result<Inmueble> {
        return try {
            val inmuebleReactivado = inmueble.copy(active = true)
            collection
                .document(inmuebleReactivado.idInmueble)
                .set(inmuebleReactivado)
                .await()

            Result.success(inmuebleReactivado)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}