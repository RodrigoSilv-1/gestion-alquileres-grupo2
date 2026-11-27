package com.example.gestionalquileres.data.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.repository.ContratoRepository
import com.example.gestionalquileres.domain.model.Contrato
import com.google.firebase.firestore.FirebaseFirestore
import com.example.gestionalquileres.domain.model.CursorPagina
import com.example.gestionalquileres.domain.model.Pagina
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreContratoRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val unidadRepository: FirestoreUnidadAlquilableRepository
) : ContratoRepository {
    private val contratosCollection = firestore.collection("contratos")

    override suspend fun obtenerContratosActivos(): AppResult<List<Contrato>> = appResultOf {
        contratosCollection
            .whereEqualTo("active", true)
            .get()
            .await()
            .documents
            .mapNotNull { doc -> doc.toObject(Contrato::class.java) }
    }

    override suspend fun obtenerContratoActivoPorInquilino(inquilinoId: String): AppResult<Contrato?> = appResultOf {
        contratosCollection
            .whereEqualTo("inquilinoId", inquilinoId)
            .whereEqualTo("active", true)
            .limit(1)
            .get()
            .await()
            .documents.firstOrNull()?.toObject(Contrato::class.java)
    }

    override suspend fun registrarContrato(contrato: Contrato): AppResult<Contrato> = appResultOf {
        val documentoRef = contratosCollection.document()
        val contratoConId = contrato.copy(idContrato = documentoRef.id, active = true)

        val unidadRef = unidadRepository.referencia(contrato.inmuebleId, contrato.unidadId)
        val inmuebleRef = firestore.collection("inmuebles").document(contrato.inmuebleId)
        val inquilinoRef = firestore.collection("inquilinos").document(contrato.inquilinoId)
        val ocupInmuebleRef = firestore.ocupacionInmueble(contrato.inmuebleId)
        val ocupInquilinoRef = firestore.ocupacionInquilino(contrato.inquilinoId)

        firestore.runTransaction { transaction ->
            // ---- Lecturas (todas antes de escribir) ----
            val unidad = transaction.get(unidadRef)
            val inmueble = transaction.get(inmuebleRef)
            val inquilino = transaction.get(inquilinoRef)
            val ocupInmueble = transaction.get(ocupInmuebleRef)
            val ocupInquilino = transaction.get(ocupInquilinoRef)

            // ---- Validaciones: si otra operación cambió algo, la transacción se reintenta y falla aquí ----
            if (inmueble.getBoolean("active") != true) {
                throw IllegalStateException("El inmueble fue dado de baja. No se puede crear el contrato.")
            }
            if (inquilino.getBoolean("active") != true) {
                throw IllegalStateException("El inquilino fue dado de baja. No se puede crear el contrato.")
            }
            if (unidad.getBoolean("estaOcupada") == true) {
                throw IllegalStateException("Esta unidad ya ha sido ocupada por otro contrato.")
            }
            if (ocupInquilino.getString("contratoId") != null) {
                throw IllegalStateException("Este inquilino ya cuenta con un contrato activo vigente en otra unidad.")
            }

            // ---- Escrituras ----
            val ocupadas = ocupInmueble.getLong("unidadesOcupadas") ?: 0L
            transaction.set(documentoRef, contratoConId)
            transaction.update(unidadRef, "estaOcupada", true)
            transaction.set(ocupInmuebleRef, mapOf("unidadesOcupadas" to ocupadas + 1))
            transaction.set(
                ocupInquilinoRef,
                mapOf("contratoId" to documentoRef.id, "nombreUnidad" to contrato.nombreUnidad)
            )
        }.await()

        contratoConId
    }

    override suspend fun finalizarContrato(
        idContrato: String,
        inmuebleId: String,
        unidadId: String
    ): AppResult<Unit> = appResultOf {
        require(idContrato.isNotBlank() && inmuebleId.isNotBlank() && unidadId.isNotBlank()) {
            "Los identificadores son obligatorios para finalizar el contrato."
        }

        val contratoRef = contratosCollection.document(idContrato)
        val unidadRef = unidadRepository.referencia(inmuebleId, unidadId)
        val ocupInmuebleRef = firestore.ocupacionInmueble(inmuebleId)

        firestore.runTransaction { transaction ->
            val contrato = transaction.get(contratoRef)
            val ocupInmueble = transaction.get(ocupInmuebleRef)

            // Evita restar dos veces si el contrato ya se finalizó desde otro dispositivo
            if (contrato.getBoolean("active") != true) {
                throw IllegalStateException("Este contrato ya estaba finalizado.")
            }
            val inquilinoId = contrato.getString("inquilinoId").orEmpty()
            val ocupadas = ocupInmueble.getLong("unidadesOcupadas") ?: 0L

            transaction.update(contratoRef, "active", false)
            transaction.update(unidadRef, "estaOcupada", false)
            transaction.set(ocupInmuebleRef, mapOf("unidadesOcupadas" to maxOf(0L, ocupadas - 1)))
            if (inquilinoId.isNotBlank()) {
                transaction.delete(firestore.ocupacionInquilino(inquilinoId))
            }
        }.await()

        Unit
    }

    /** MIGRACIÓN ÚNICA para datos anteriores a las marcas de ocupación. Es idempotente. */
    override suspend fun reconstruirOcupacion(): AppResult<Int> = appResultOf {
        val activos = contratosCollection.whereEqualTo("active", true).get().await()
            .documents.mapNotNull { it.toObject(Contrato::class.java) }

        val escrituras = mutableListOf<Pair<DocumentReference, Map<String, Any>>>()

        activos.groupBy { it.inmuebleId }.forEach { (inmuebleId, contratos) ->
            escrituras += firestore.ocupacionInmueble(inmuebleId) to mapOf("unidadesOcupadas" to contratos.size)
        }
        activos.forEach { contrato ->
            escrituras += firestore.ocupacionInquilino(contrato.inquilinoId) to
                    mapOf("contratoId" to contrato.idContrato, "nombreUnidad" to contrato.nombreUnidad)
        }

        escrituras.chunked(400).forEach { grupo ->
            val lote = firestore.batch()
            grupo.forEach { (ref, datos) -> lote.set(ref, datos) }
            lote.commit().await()
        }

        activos.size
    }

    override suspend fun obtenerContratosActivosPagina(
        limite: Int,
        despues: CursorPagina?
    ): AppResult<Pagina<Contrato>> = appResultOf {
        contratosCollection
            .whereEqualTo("active", true)
            .orderBy("fechaInicio", Query.Direction.DESCENDING)   // requiere índice compuesto (ver A5)
            .leerPagina(limite, despues) { doc -> doc.toObject(Contrato::class.java) }
    }
}