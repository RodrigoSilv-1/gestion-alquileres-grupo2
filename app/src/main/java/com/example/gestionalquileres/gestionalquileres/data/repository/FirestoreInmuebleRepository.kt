package com.example.gestionalquileres.data.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.repository.InmuebleRepository
import com.example.gestionalquileres.domain.model.Inmueble
import com.example.gestionalquileres.domain.model.UnidadAlquilable
import com.example.gestionalquileres.domain.model.calcularClaveDireccion
import com.example.gestionalquileres.domain.model.claveDireccion
import com.google.firebase.firestore.FirebaseFirestore
import com.example.gestionalquileres.domain.model.MAX_UNIDADES_POR_INMUEBLE
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await
import com.example.gestionalquileres.domain.model.CursorPagina
import com.example.gestionalquileres.domain.model.Pagina
import javax.inject.Inject

class FirestoreInmuebleRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : InmuebleRepository {

    private val collection = firestore.collection(COLECCION)
    private val direcciones = firestore.collection(COLECCION_DIRECCIONES)

    /**
     * Registra el inmueble, reserva su dirección y genera sus unidades alquilables en UNA transacción:
     * o se guarda todo o no se guarda nada, y dos registros simultáneos de la misma dirección no pasan.
     * Máximo: 1 reserva + 1 inmueble + 400 unidades = 402 escrituras (el límite de Firestore es 500).
     */
    override suspend fun registrar(inmueble: Inmueble): AppResult<Inmueble> = appResultOf {
        require(inmueble.numeroPisos > 0 && inmueble.unidadesPorPiso > 0) {
            "Debe haber al menos 1 piso y 1 unidad por piso."
        }
        require(inmueble.numeroPisos.toLong() * inmueble.unidadesPorPiso <= MAX_UNIDADES_POR_INMUEBLE) {
            "Un inmueble no puede tener más de $MAX_UNIDADES_POR_INMUEBLE unidades (pisos × unidades por piso)."
        }

        val documento = collection.document()
        val nuevo = inmueble.copy(idInmueble = documento.id, active = true)
        val unidades = documento.collection(SUBCOLECCION_UNIDADES)
        val reservaRef = direcciones.document(nuevo.claveDireccion)

        firestore.runTransaction { transaction ->
            // En una transacción, las lecturas van antes que las escrituras
            if (transaction.get(reservaRef).exists()) {
                throw IllegalStateException(MENSAJE_DIRECCION_DUPLICADA)
            }

            transaction.set(reservaRef, mapOf("inmuebleId" to documento.id))
            transaction.set(documento, nuevo)
            for (piso in 1..nuevo.numeroPisos) {
                for (numero in 1..nuevo.unidadesPorPiso) {
                    val ref = unidades.document()
                    transaction.set(
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
        }.await()

        nuevo
    }

    // ---------- Lecturas (solo inmuebles activos) ----------

    override suspend fun obtenerTodos(): AppResult<List<Inmueble>> =
        consultar(collection.whereEqualTo("active", true))

    override suspend fun buscarPorDireccion(
        distrito: String,
        direccion: String,
        numeroDireccion: String,
        excluirId: String?
    ): AppResult<Inmueble?> = appResultOf {
        val clave = calcularClaveDireccion(distrito, direccion, numeroDireccion)

        // Una sola lectura: la reserva de esa dirección (existe también para inmuebles dados de baja)
        val reserva = direcciones.document(clave).get().await()
        if (!reserva.exists()) return@appResultOf null

        val idDueno = reserva.getString("inmuebleId") ?: return@appResultOf null
        if (idDueno == excluirId) return@appResultOf null

        collection.document(idDueno).get().await()
            .toObject(Inmueble::class.java)
            ?.copy(idInmueble = idDueno)
    }

    // ---------- Escrituras ----------

    override suspend fun actualizar(inmueble: Inmueble): AppResult<Inmueble> = appResultOf {
        require(inmueble.idInmueble.isNotBlank()) { "El ID del inmueble es obligatorio." }

        val inmuebleRef = collection.document(inmueble.idInmueble)
        val claveNueva = inmueble.claveDireccion
        val reservaNuevaRef = direcciones.document(claveNueva)

        firestore.runTransaction { transaction ->
            // Lecturas primero
            val anterior = transaction.get(inmuebleRef).toObject(Inmueble::class.java)
            val reservaNueva = transaction.get(reservaNuevaRef)

            val duenoActual = reservaNueva.getString("inmuebleId")
            if (reservaNueva.exists() && duenoActual != inmueble.idInmueble) {
                throw IllegalStateException(MENSAJE_DIRECCION_DUPLICADA)
            }

            // Escrituras: si la dirección cambió, se libera la reserva anterior
            val claveAnterior = anterior?.claveDireccion
            if (claveAnterior != null && claveAnterior != claveNueva) {
                transaction.delete(direcciones.document(claveAnterior))
            }
            transaction.set(reservaNuevaRef, mapOf("inmuebleId" to inmueble.idInmueble))
            transaction.set(inmuebleRef, inmueble)
        }.await()

        inmueble
    }

    override suspend fun darDeBaja(idInmueble: String): AppResult<Unit> = appResultOf {
        require(idInmueble.isNotBlank()) { "El ID del inmueble es obligatorio." }

        val inmuebleRef = collection.document(idInmueble)
        val ocupacionRef = firestore.ocupacionInmueble(idInmueble)

        firestore.runTransaction { transaction ->
            val inmueble = transaction.get(inmuebleRef)
            val ocupacion = transaction.get(ocupacionRef)

            if (!inmueble.exists()) throw IllegalStateException("El inmueble no existe.")
            // Verificación y baja en la MISMA transacción: si un contrato se registra a la vez, uno de los dos reintenta
            if ((ocupacion.getLong("unidadesOcupadas") ?: 0L) > 0L) {
                throw IllegalStateException(MENSAJE_BAJA_BLOQUEADA)
            }

            transaction.update(inmuebleRef, "active", false)
        }.await()

        Unit
    }

    override suspend fun reactivarYActualizar(inmueble: Inmueble): AppResult<Inmueble> = appResultOf {
        require(inmueble.idInmueble.isNotBlank()) { "El ID del inmueble es obligatorio." }

        // update() parcial: numeroPisos y unidadesPorPiso NO se modifican
        collection.document(inmueble.idInmueble).update(
            mapOf<String, Any?>(
                "active" to true,
                "distrito" to inmueble.distrito,
                "direccion" to inmueble.direccion,
                "numeroDireccion" to inmueble.numeroDireccion,
                "codigoPostal" to inmueble.codigoPostal,
                "fotografia" to inmueble.fotografia
            )
        ).await()

        inmueble.copy(active = true)
    }

    override suspend fun obtenerPagina(
        limite: Int,
        despues: CursorPagina?
    ): AppResult<Pagina<Inmueble>> = appResultOf {
        collection
            .whereEqualTo("active", true)
            .orderBy("distrito")                       // requiere índice compuesto (ver A5)
            .leerPagina(limite, despues) { doc ->
                doc.toObject(Inmueble::class.java)?.copy(idInmueble = doc.id)
            }
    }

    // ---------- Helpers privados ----------

    private suspend fun consultar(query: Query): AppResult<List<Inmueble>> =
        appResultOf { query.get().await().aInmuebles() }

    private fun QuerySnapshot.aInmuebles(): List<Inmueble> =
        documents.mapNotNull { doc -> doc.toObject(Inmueble::class.java)?.copy(idInmueble = doc.id) }

    private companion object {
        const val COLECCION = "inmuebles"
        const val COLECCION_DIRECCIONES = "direccionesInmuebles"
        const val SUBCOLECCION_UNIDADES = "unidadesAlquilables"
        const val MENSAJE_DIRECCION_DUPLICADA = "Ya existe un inmueble registrado en esa dirección."
        const val MENSAJE_BAJA_BLOQUEADA = "No se puede dar de baja: el inmueble tiene unidades alquiladas. Finaliza primero los contratos vigentes."
    }
}