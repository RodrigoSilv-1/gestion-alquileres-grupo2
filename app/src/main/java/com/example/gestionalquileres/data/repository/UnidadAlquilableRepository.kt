package com.example.gestionalquileres.data.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.UnidadAlquilable
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UnidadAlquilableRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private fun unidades(inmuebleId: String): CollectionReference =
        firestore.collection("inmuebles")
            .document(inmuebleId)
            .collection("unidadesAlquilables")

    suspend fun obtenerUnidadesPorInmueble(inmuebleId: String): AppResult<List<UnidadAlquilable>> =
        appResultOf {
            unidades(inmuebleId).get().await().documents.mapNotNull { doc ->
                doc.toObject(UnidadAlquilable::class.java)?.copy(id = doc.id)
            }
        }

    suspend fun tieneUnidadesOcupadas(inmuebleId: String): AppResult<Boolean> =
        appResultOf {
            !unidades(inmuebleId)
                .whereEqualTo("estaOcupada", true)
                .limit(1)
                .get()
                .await()
                .isEmpty
        }

    suspend fun actualizarPrecioRenta(
        inmuebleId: String,
        unidadId: String,
        nuevoPrecio: Double
    ): AppResult<Unit> = appResultOf {
        unidades(inmuebleId).document(unidadId).update("precioRenta", nuevoPrecio).await()
        Unit
    }

    /** Referencia al documento de una unidad (la usan las transacciones de ContratoRepository). */
    fun referencia(inmuebleId: String, unidadId: String): DocumentReference =
        unidades(inmuebleId).document(unidadId)
}