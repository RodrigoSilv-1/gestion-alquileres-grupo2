package com.example.gestionalquileres.domain.model

const val MAX_BASE64_FOTO_COMPLETA = 300_000

enum class TipoFoto(val prefijo: String) {
    INMUEBLE("inmueble"),
    INQUILINO("inquilino")
}

interface FotoRepository {
    suspend fun guardar(tipo: TipoFoto, idPropietario: String, base64: String): AppResult<Unit>
    suspend fun obtener(tipo: TipoFoto, idPropietario: String): AppResult<String?>
    suspend fun eliminar(tipo: TipoFoto, idPropietario: String): AppResult<Unit>
}

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