package com.example.gestionalquileres.data.repository

import com.example.gestionalquileres.domain.model.Inquilino
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class InquilinoRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // Referencia a la coleccion principal en Firestore
    private val collection = firestore.collection("inquilinos")

    // Guarda un nuevo inquilino generando un ID unico en Firestore
    suspend fun registrar(inquilino: Inquilino): Result<Inquilino> {
        return try {
            val documento = collection.document()

            val nuevoInquilino = inquilino.copy(
                idInquilino = documento.id,
                active = true
            )

            documento.set(nuevoInquilino).await()

            Result.success(nuevoInquilino)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    // Trae solo los inquilinos que estan activos
    suspend fun obtenerTodos(): Result<List<Inquilino>> {
        return try {
            val snapshot = collection
                .whereEqualTo("active", true)
                .get()
                .await()

            val inquilinos = snapshot.documents.mapNotNull { documento ->
                documento.toObject(Inquilino::class.java)
            }

            Result.success(inquilinos)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    // Filtra inquilinos activos por coincidencia exacta de DNI
    suspend fun obtenerPorDni(dni: String): Result<List<Inquilino>> {
        return try {
            val snapshot = collection
                .whereEqualTo("dni", dni)
                .whereEqualTo("active", true)
                .get()
                .await()

            val inquilinos = snapshot.documents.mapNotNull { documento ->
                documento.toObject(Inquilino::class.java)
            }

            Result.success(inquilinos)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    // Busca inquilinos activos por coincidencia de texto en el nombre
    suspend fun obtenerPorNombre(nombre: String): Result<List<Inquilino>> {
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

            Result.success(inquilinos)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    // Sobrescribe los datos de un inquilino existente usando su ID
    suspend fun actualizar(inquilino: Inquilino): Result<Inquilino> {
        return try {
            if (inquilino.idInquilino.isBlank()) {
                return Result.failure(
                    Exception("El ID del inquilino es obligatorio.")
                )
            }

            collection
                .document(inquilino.idInquilino)
                .set(inquilino)
                .await()

            Result.success(inquilino)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    // Baja logica: no borra el documento, solo marca active = false
    suspend fun darDeBaja(idInquilino: String): Result<Unit> {
        return try {
            if (idInquilino.isBlank()) {
                return Result.failure(
                    Exception("El ID del inquilino es obligatorio.")
                )
            }

            collection
                .document(idInquilino)
                .update("active", false)
                .await()

            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    // Comprueba si un DNI ya existe en la base de datos (activo o inactivo)
    suspend fun buscarPorDniCualquiera(dni: String): Result<Inquilino?> {
        return try {
            val snapshot = collection
                .whereEqualTo("dni", dni)
                .limit(1)
                .get()
                .await()

            val inquilino = snapshot.documents.firstOrNull()?.toObject(Inquilino::class.java)
            Result.success(inquilino)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    // Vuelve a activar un inquilino dado de baja y actualiza su informacion
    suspend fun reactivarYActualizar(inquilino: Inquilino): Result<Inquilino> {
        return try {
            val inquilinoReactivado = inquilino.copy(active = true)
            collection
                .document(inquilinoReactivado.idInquilino)
                .set(inquilinoReactivado)
                .await()

            Result.success(inquilinoReactivado)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}