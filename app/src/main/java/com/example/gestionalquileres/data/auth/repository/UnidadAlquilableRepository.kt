package com.example.gestionalquileres.data.auth.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.UnidadAlquilable
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UnidadAlquilableRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun obtenerUnidadesPorInmueble(inmuebleId: String): AppResult<List<UnidadAlquilable>> {
        return try {
            val snapshot = firestore.collection("inmuebles")
                .document(inmuebleId)
                .collection("unidadesAlquilables")
                .get()
                .await()

            val unidades = snapshot.toObjects(UnidadAlquilable::class.java)
            AppResult.Exito(unidades)
        } catch (e: Exception) {
            AppResult.Error(e)
        }
    }

    suspend fun actualizarPrecioRenta(inmuebleId: String, unidadId: String, nuevoPrecio: Double): AppResult<Unit> {
        return try {
            firestore.collection("inmuebles")
                .document(inmuebleId)
                .collection("unidadesAlquilables")
                .document(unidadId)
                .update("precioRenta", nuevoPrecio)
                .await()
            AppResult.Exito(Unit)
        } catch (e: Exception) {
            AppResult.Error(e)
        }
    }
}