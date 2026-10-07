package com.example.gestionalquileres.data.auth.repository

import com.example.gestionalquileres.domain.model.UnidadAlquilable
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UnidadAlquilableRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    // Obtiene todas las unidades alquilables pertenecientes a un inmueble específico
    suspend fun obtenerUnidadesPorInmueble(inmuebleId: String): Result<List<UnidadAlquilable>> {
        return try {
            val snapshot = firestore.collection("inmuebles")
                .document(inmuebleId)
                .collection("unidadesAlquilables")
                .get()
                .await()

            val unidades = snapshot.toObjects(UnidadAlquilable::class.java)
            Result.success(unidades)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizarPrecioRenta(inmuebleId: String, unidadId: String, nuevoPrecio: Double): Result<Unit> {
        return try {
            firestore.collection("inmuebles")
                .document(inmuebleId)
                .collection("unidadesAlquilables")
                .document(unidadId)
                .update("precioRenta", nuevoPrecio)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}