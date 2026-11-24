package com.example.gestionalquileres.data.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.repository.InquilinoRepository
import com.example.gestionalquileres.domain.model.Inquilino
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreInquilinoRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : InquilinoRepository {

    private val collection = firestore.collection(COLECCION)

    // ---------- Lecturas ----------

    /** Solo inquilinos activos. */
    override suspend fun obtenerTodos(): AppResult<List<Inquilino>> = appResultOf {
        collection.whereEqualTo("active", true).get().await()
            .documents
            .mapNotNull { doc -> doc.toObject(Inquilino::class.java)?.copy(idInquilino = doc.id) }
    }

    /** Busca por DNI entre activos E inactivos (para detectar inquilinos dados de baja). */
    override suspend fun buscarPorDniCualquiera(dni: String): AppResult<Inquilino?> = appResultOf {
        collection.whereEqualTo("dni", dni).limit(1).get().await()
            .documents
            .firstOrNull()
            ?.let { doc -> doc.toObject(Inquilino::class.java)?.copy(idInquilino = doc.id) }
    }

    // ---------- Escrituras ----------

    override suspend fun registrar(inquilino: Inquilino): AppResult<Inquilino> = appResultOf {
        val documento = collection.document()
        val nuevo = inquilino.copy(idInquilino = documento.id, active = true)
        documento.set(nuevo).await()
        nuevo
    }

    override suspend fun actualizar(inquilino: Inquilino): AppResult<Inquilino> = appResultOf {
        require(inquilino.idInquilino.isNotBlank()) { "El ID del inquilino es obligatorio." }
        collection.document(inquilino.idInquilino).set(inquilino).await()
        inquilino
    }

    override suspend fun darDeBaja(idInquilino: String): AppResult<Unit> = appResultOf {
        require(idInquilino.isNotBlank()) { "El ID del inquilino es obligatorio." }

        val inquilinoRef = collection.document(idInquilino)
        val ocupacionRef = firestore.ocupacionInquilino(idInquilino)

        firestore.runTransaction { transaction ->
            val inquilino = transaction.get(inquilinoRef)
            val ocupacion = transaction.get(ocupacionRef)

            if (!inquilino.exists()) throw IllegalStateException("El inquilino no existe.")
            if (ocupacion.getString("contratoId") != null) {
                val unidad = ocupacion.getString("nombreUnidad") ?: "una unidad"
                throw IllegalStateException(
                    "No se puede dar de baja: el inquilino tiene un contrato vigente en $unidad. " +
                            "Finaliza primero el contrato."
                )
            }

            transaction.update(inquilinoRef, "active", false)
        }.await()

        Unit
    }

    override suspend fun reactivarYActualizar(inquilino: Inquilino): AppResult<Inquilino> = appResultOf {
        require(inquilino.idInquilino.isNotBlank()) { "El ID del inquilino es obligatorio." }
        val reactivado = inquilino.copy(active = true)
        collection.document(reactivado.idInquilino).set(reactivado).await()
        reactivado
    }

    private companion object {
        const val COLECCION = "inquilinos"
    }
}