package com.example.gestionalquileres.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

data class FotoRequest(
    val base64: String
)

data class FotoResponse(
    val base64: String? = null
)

interface FotoApiService {

    @PUT("fotos/{tipo}/{idPropietario}")
    suspend fun guardarFoto(
        @Path("tipo") tipo: String,
        @Path("idPropietario") idPropietario: String,
        @Body body: FotoRequest
    ): Response<Unit>

    @GET("fotos/{tipo}/{idPropietario}")
    suspend fun obtenerFoto(
        @Path("tipo") tipo: String,
        @Path("idPropietario") idPropietario: String
    ): Response<FotoResponse>

    @DELETE("fotos/{tipo}/{idPropietario}")
    suspend fun eliminarFoto(
        @Path("tipo") tipo: String,
        @Path("idPropietario") idPropietario: String
    ): Response<Unit>
}