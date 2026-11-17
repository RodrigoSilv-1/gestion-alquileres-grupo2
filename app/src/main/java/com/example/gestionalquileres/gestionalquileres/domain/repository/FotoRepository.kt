package com.example.gestionalquileres.domain.repository

import com.example.gestionalquileres.domain.model.AppResult

/** Tope de la foto grande en Base64 (≈ 150 KB binarios). Protege el límite de 1 MB por documento. */
const val MAX_BASE64_FOTO_COMPLETA = 300_000

enum class TipoFoto(val prefijo: String) {
    INMUEBLE("inmueble"),
    INQUILINO("inquilino")
}

/** Guarda aparte la foto grande; la miniatura vive en el propio inmueble/inquilino. */
interface FotoRepository {
    suspend fun guardar(tipo: TipoFoto, idPropietario: String, base64: String): AppResult<Unit>
    suspend fun obtener(tipo: TipoFoto, idPropietario: String): AppResult<String?>
    suspend fun eliminar(tipo: TipoFoto, idPropietario: String): AppResult<Unit>
}

/**
 * Deja la colección "fotos" coherente tras guardar un registro:
 * - [completa] != null  → el usuario eligió una foto nueva: se guarda.
 * - sin miniatura y [teniaFoto] → el usuario la quitó: se borra.
 * - en otro caso no se toca nada.
 * Devuelve false si la operación falló.
 */
suspend fun FotoRepository.sincronizar(
    tipo: TipoFoto,
    idPropietario: String,
    miniatura: String?,
    completa: String?,
    teniaFoto: Boolean
): Boolean = when {
    completa != null -> guardar(tipo, idPropietario, completa) is AppResult.Exito
    miniatura == null && teniaFoto -> eliminar(tipo, idPropietario) is AppResult.Exito
    else -> true
}