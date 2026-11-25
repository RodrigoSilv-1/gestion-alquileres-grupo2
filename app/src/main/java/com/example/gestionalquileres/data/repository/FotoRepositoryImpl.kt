package com.example.gestionalquileres.data.repository

import com.example.gestionalquileres.data.remote.FotoApiService
import com.example.gestionalquileres.data.remote.FotoRequest
import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.FotoRepository
import com.example.gestionalquileres.domain.model.MAX_BASE64_FOTO_COMPLETA
import com.example.gestionalquileres.domain.model.TipoFoto
import javax.inject.Inject

class FotoRepositoryImpl @Inject constructor(
    private val fotoApiService: FotoApiService
) : FotoRepository {

    override suspend fun guardar(
        tipo: TipoFoto,
        idPropietario: String,
        base64: String
    ): AppResult<Unit> {
        return try {
            require(idPropietario.isNotBlank()) { "El ID del propietario es obligatorio." }
            require(base64.length <= MAX_BASE64_FOTO_COMPLETA) {
                "La foto excede el tamaño máximo permitido."
            }

            val response = fotoApiService.guardarFoto(
                tipo = tipo.prefijo,
                idPropietario = idPropietario,
                body = FotoRequest(base64 = base64)
            )

            if (response.isSuccessful) {
                AppResult.Exito(Unit)
            } else {
                AppResult.Error(
                    Exception("Error al guardar la foto (código ${response.code()}).")
                )
            }
        } catch (e: Exception) {
            AppResult.Error(e)
        }
    }

    override suspend fun obtener(
        tipo: TipoFoto,
        idPropietario: String
    ): AppResult<String?> {
        return try {
            require(idPropietario.isNotBlank()) { "El ID del propietario es obligatorio." }

            val response = fotoApiService.obtenerFoto(
                tipo = tipo.prefijo,
                idPropietario = idPropietario
            )

            when {
                response.isSuccessful -> AppResult.Exito(response.body()?.base64)
                response.code() == 404 -> AppResult.Exito(null)
                else -> AppResult.Error(
                    Exception("Error al obtener la foto (código ${response.code()}).")
                )
            }
        } catch (e: Exception) {
            AppResult.Error(e)
        }
    }

    override suspend fun eliminar(
        tipo: TipoFoto,
        idPropietario: String
    ): AppResult<Unit> {
        return try {
            require(idPropietario.isNotBlank()) { "El ID del propietario es obligatorio." }

            val response = fotoApiService.eliminarFoto(
                tipo = tipo.prefijo,
                idPropietario = idPropietario
            )

            when {
                response.isSuccessful || response.code() == 404 -> AppResult.Exito(Unit)
                else -> AppResult.Error(
                    Exception("Error al eliminar la foto (código ${response.code()}).")
                )
            }
        } catch (e: Exception) {
            AppResult.Error(e)
        }
    }
}