package com.example.gestionalquileres.data.auth.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.UnidadAlquilable
import com.google.firebase.firestore.CollectionReference
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

    suspend fun actualizarPrecioRenta(
        inmuebleId: String,
        unidadId: String,
        nuevoPrecio: Double
    ): AppResult<Unit> = appResultOf {
        unidades(inmuebleId).document(unidadId).update("precioRenta", nuevoPrecio).await()
        Unit
    }
}