package com.example.gestionalquileres.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    // --- INQUILINOS ---
    @Headers("Prefer: return=representation")
    @GET("inquilinos")
    suspend fun obtenerInquilinos(): List<InquilinoDto>

    @Headers("Prefer: return=representation")
    @POST("inquilinos")
    suspend fun registrarInquilino(@Body inquilino: InquilinoDto): List<InquilinoDto>

    @PATCH("inquilinos")
    suspend fun actualizarInquilino(
        @Query("id") idFiltro: String, // Pasar desde el Repo como: "eq.$id"
        @Body inquilino: InquilinoDto
    ): List<InquilinoDto>

    @PATCH("inquilinos")
    suspend fun darDeBajaInquilino(
        @Query("id") idFiltro: String, // Pasar como: "eq.$id"
        @Body body: DarDeBajaRequest = DarDeBajaRequest(active = false)
    )


    // --- INMUEBLES ---
    @Headers("Prefer: return=representation")
    @GET("inmuebles")
    suspend fun obtenerInmuebles(): List<InmuebleDto>

    @Headers("Prefer: return=representation")
    @POST("inmuebles")
    suspend fun registrarInmueble(@Body inmueble: InmuebleDto): List<InmuebleDto>

    @PATCH("inmuebles")
    suspend fun actualizarInmueble(
        @Query("id") idFiltro: String, // Pasar como: "eq.$id"
        @Body inmueble: InmuebleDto
    ): List<InmuebleDto>

    @PATCH("inmuebles")
    suspend fun darDeBajaInmueble(
        @Query("id") idFiltro: String, // Pasar como: "eq.$id"
        @Body body: DarDeBajaRequest = DarDeBajaRequest(active = false)
    )


    // --- CONTRATOS ---
    @Headers("Prefer: return=representation")
    @GET("contratos")
    suspend fun obtenerContratos(): List<ContratoDto>

    @POST("contratos")
    suspend fun registrarContrato(@Body contrato: ContratoDto): List<ContratoDto>

    @PATCH("contratos")
    suspend fun finalizarContrato(
        @Query("id") idFiltro: String, // Pasar como: "eq.$idContrato"
        @Body body: DarDeBajaRequest = DarDeBajaRequest(active = false)
    )


    // --- RECIBOS ---
    @Headers("Prefer: return=representation")
    @GET("recibos")
    suspend fun obtenerRecibos(): List<ReciboDto>


    @Headers("Prefer: return=representation")
    @POST("recibos")
    suspend fun generarRecibo(@Body recibo: ReciboDto): List<ReciboDto>

    @PATCH("recibos")
    suspend fun actualizarEstadoPago(
        @Query("id") idFiltro: String, // Pasar como: "eq.$idRecibo"
        @Body body: ActualizarEstadoRequest
    )


    // --- UNIDADES ALQUILABLES ---
    // Nota: Si tu tabla en Supabase se llama "unidades_alquilables", cambia "unidades" por "unidades_alquilables"
    @GET("unidades")
    suspend fun obtenerUnidadesPorInmueble(
        @Query("inmuebleId") inmuebleIdFiltro: String // Pasar como: "eq.$inmuebleId"
    ): List<UnidadAlquilableDto>

    @Headers("Prefer: return=representation")
    @POST("unidades")
    suspend fun registrarUnidad(@Body unidad: UnidadAlquilableDto): List<UnidadAlquilableDto>

    @PATCH("unidades")
    suspend fun actualizarPrecioRenta(
        @Query("id") unidadIdFiltro: String, // Pasar como: "eq.$unidadId"
        @Body body: ActualizarPrecioRequest
    )

    @PATCH("unidades")
    suspend fun actualizarEstadoOcupacion(
        @Query("id") unidadIdFiltro: String, // Pasar como: "eq.$unidadId"
        @Body body: ActualizarOcupacionRequest
    )
}

// Clases de petición (Request DTOs) adaptadas a Supabase
data class DarDeBajaRequest(
    val active: Boolean = false // Asegúrate de que tu columna de estado en Supabase se llame "active"
)

data class ActualizarEstadoRequest(
    val estadoPago: String
)

data class ActualizarPrecioRequest(
    val precioRenta: Double
)

data class ActualizarOcupacionRequest(
    val estaOcupada: Boolean
)