package com.example.gestionalquileres.domain.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Inmueble

interface InmuebleRepository {
    /** Registra el inmueble y genera sus unidades alquilables de forma atómica. */
    suspend fun registrar(inmueble: Inmueble): AppResult<Inmueble>

    /** Solo inmuebles activos. */
    suspend fun obtenerTodos(): AppResult<List<Inmueble>>

    suspend fun actualizar(inmueble: Inmueble): AppResult<Inmueble>
    suspend fun darDeBaja(idInmueble: String): AppResult<Unit>
    suspend fun reactivarYActualizar(inmueble: Inmueble): AppResult<Inmueble>
}