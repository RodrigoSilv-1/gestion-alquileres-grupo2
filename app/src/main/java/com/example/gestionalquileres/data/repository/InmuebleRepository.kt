package com.example.gestionalquileres.data.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Inmueble
import com.example.gestionalquileres.domain.model.UnidadAlquilable
import com.google.firebase.firestore.FirebaseFirestore
import com.example.gestionalquileres.domain.model.MAX_UNIDADES_POR_INMUEBLE
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class InmuebleRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val collection = firestore.collection(COLECCION)

    /**
     * Registra el inmueble y genera sus unidades alquilables en UN SOLO WriteBatch,
     * así el registro es atómico: o se guarda todo o no se guarda nada.
     * Máximo: 1 inmueble + 400 unidades = 401 escrituras (el límite de Firestore es 500).
     */
    suspend fun registrar(inmueble: Inmueble): AppResult<Inmueble> = appResultOf {
        require(inmueble.numeroPisos > 0 && inmueble.unidadesPorPiso > 0) {
            "Debe haber al menos 1 piso y 1 unidad por piso."
        }
        require(inmueble.numeroPisos.toLong() * inmueble.unidadesPorPiso <= MAX_UNIDADES_POR_INMUEBLE) {
            "Un inmueble no puede tener más de $MAX_UNIDADES_POR_INMUEBLE unidades (pisos × unidades por piso)."
        }

        val documento = collection.document()
        val nuevo = inmueble.copy(idInmueble = documento.id, active = true)
        val unidades = documento.collection(SUBCOLECCION_UNIDADES)

        val lote = firestore.batch()
        lote.set(documento, nuevo)
        for (piso in 1..nuevo.numeroPisos) {
            for (numero in 1..nuevo.unidadesPorPiso) {
                val ref = unidades.document()
                lote.set(
                    ref,
                    UnidadAlquilable(
                        id = ref.id,
                        inmuebleId = documento.id,
                        piso = piso,
                        numeroUnidad = numero,
                        nombreFormateado = "$piso${numero.toString().padStart(2, '0')}", // "101"
                        estaOcupada = false
                    )
                )
            }
        }
        lote.commit().await()

        nuevo
    }

    // ---------- Lecturas (solo inmuebles activos) ----------

    suspend fun obtenerTodos(): AppResult<List<Inmueble>> =
        consultar(collection.whereEqualTo("active", true))

    // ---------- Escrituras ----------

    suspend fun actualizar(inmueble: Inmueble): AppResult<Inmueble> = appResultOf {
        require(inmueble.idInmueble.isNotBlank()) { "El ID del inmueble es obligatorio." }
        collection.document(inmueble.idInmueble).set(inmueble).await()
        inmueble
    }

    suspend fun darDeBaja(idInmueble: String): AppResult<Unit> = appResultOf {
        require(idInmueble.isNotBlank()) { "El ID del inmueble es obligatorio." }
        collection.document(idInmueble).update("active", false).await()
        Unit
    }

    suspend fun reactivarYActualizar(inmueble: Inmueble): AppResult<Inmueble> = appResultOf {
        require(inmueble.idInmueble.isNotBlank()) { "El ID del inmueble es obligatorio." }
        val reactivado = inmueble.copy(active = true)
        collection.document(reactivado.idInmueble).set(reactivado).await()
        reactivado
    }

    // ---------- Helpers privados ----------

    private suspend fun consultar(query: Query): AppResult<List<Inmueble>> =
        appResultOf { query.get().await().aInmuebles() }

    private fun QuerySnapshot.aInmuebles(): List<Inmueble> =
        documents.mapNotNull { doc -> doc.toObject(Inmueble::class.java)?.copy(idInmueble = doc.id) }

    private companion object {
        const val COLECCION = "inmuebles"
        const val SUBCOLECCION_UNIDADES = "unidadesAlquilables"
    }
}