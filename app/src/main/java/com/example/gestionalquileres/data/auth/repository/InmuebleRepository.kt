package com.example.gestionalquileres.data.auth.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Inmueble
import com.example.gestionalquileres.domain.model.UnidadAlquilable
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class InmuebleRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val collection = firestore.collection(COLECCION)

    /**
     * Registra el inmueble y genera sus unidades alquilables.
     * Se escribe por lotes (WriteBatch): el inmueble va en el primer lote junto
     * con las primeras unidades, así nunca quedan unidades huérfanas.
     */
    suspend fun registrar(inmueble: Inmueble): AppResult<Inmueble> = appResultOf {
        val documento = collection.document()
        val nuevo = inmueble.copy(idInmueble = documento.id, active = true)
        val unidades = documento.collection(SUBCOLECCION_UNIDADES)

        val escrituras = buildList<Pair<DocumentReference, Any>> {
            add(documento to nuevo)
            for (piso in 1..nuevo.numeroPisos) {
                for (numero in 1..nuevo.unidadesPorPiso) {
                    val ref = unidades.document()
                    add(
                        ref to UnidadAlquilable(
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
        }

        escrituras.chunked(MAX_ESCRITURAS_POR_LOTE).forEach { grupo ->
            firestore.batch().apply { grupo.forEach { (ref, dato) -> set(ref, dato) } }.commit().await()
        }

        nuevo
    }

    // ---------- Lecturas (solo inmuebles activos) ----------

    suspend fun obtenerTodos(): AppResult<List<Inmueble>> =
        consultar(collection.whereEqualTo("active", true))

    suspend fun obtenerPorCodigoPostal(codigoPostal: String): AppResult<List<Inmueble>> =
        consultar(
            collection
                .whereEqualTo("codigoPostal", codigoPostal)
                .whereEqualTo("active", true)
        )

    /** Firestore no soporta "contiene", por eso se filtra el distrito en memoria. */
    suspend fun obtenerPorDistrito(distrito: String): AppResult<List<Inmueble>> = appResultOf {
        collection.whereEqualTo("active", true).get().await()
            .aInmuebles()
            .filter { it.distrito.contains(distrito, ignoreCase = true) }
    }

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
        const val MAX_ESCRITURAS_POR_LOTE = 400 // Firestore permite 500 por lote
    }
}