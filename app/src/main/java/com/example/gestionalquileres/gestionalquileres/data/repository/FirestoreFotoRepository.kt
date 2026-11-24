package com.example.gestionalquileres.data.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** Guarda la foto grande en "fotos/{tipo}_{id}", fuera del documento que se lista. */
class FirestoreFotoRepository @Inject constructor(
    firestore: FirebaseFirestore
) : FotoRepository {

    private val coleccion = firestore.collection(COLECCION)

    private fun referencia(tipo: TipoFoto, idPropietario: String): DocumentReference =
        coleccion.document("${tipo.prefijo}_$idPropietario")

    override suspend fun guardar(
        tipo: TipoFoto,
        idPropietario: String,
        base64: String
    ): AppResult<Unit> = appResultOf {
        require(idPropietario.isNotBlank()) { "El ID del propietario de la foto es obligatorio." }
        require(base64.length <= MAX_BASE64_FOTO_COMPLETA) { "La fotografía supera el tamaño permitido." }
        referencia(tipo, idPropietario).set(mapOf(CAMPO_DATOS to base64)).await()
        Unit
    }

    override suspend fun obtener(tipo: TipoFoto, idPropietario: String): AppResult<String?> = appResultOf {
        referencia(tipo, idPropietario).get().await().getString(CAMPO_DATOS)
    }

    override suspend fun eliminar(tipo: TipoFoto, idPropietario: String): AppResult<Unit> = appResultOf {
        referencia(tipo, idPropietario).delete().await()
        Unit
    }

    private companion object {
        const val COLECCION = "fotos"
        const val CAMPO_DATOS = "datos"
    }
}