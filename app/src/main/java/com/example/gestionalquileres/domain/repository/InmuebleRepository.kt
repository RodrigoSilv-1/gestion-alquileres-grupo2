package com.example.gestionalquileres.domain.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Inmueble

interface InmuebleRepository {
    /** Registra el inmueble y genera sus unidades alquilables de forma atómica. */
    suspend fun registrar(inmueble: Inmueble): AppResult<Inmueble>

    /** Solo inmuebles activos. */
    suspend fun obtenerTodos(): AppResult<List<Inmueble>>

    /**
     * Busca un inmueble (ACTIVO O DADO DE BAJA) con la misma dirección.
     * Si hay varios, prefiere el activo. [excluirId] ignora el propio inmueble al editar.
     */
    suspend fun buscarPorDireccion(
        distrito: String,
        direccion: String,
        numeroDireccion: String,
        excluirId: String? = null
    ): AppResult<Inmueble?>

    suspend fun actualizar(inmueble: Inmueble): AppResult<Inmueble>
    suspend fun darDeBaja(idInmueble: String): AppResult<Unit>

    /**
     * Reactiva el inmueble y actualiza SOLO sus datos descriptivos (dirección, código postal,
     * foto). Pisos y unidades por piso no se tocan: las unidades ya existen.
     */
    suspend fun reactivarYActualizar(inmueble: Inmueble): AppResult<Inmueble>
}