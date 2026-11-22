package com.example.gestionalquileres.domain.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Inmueble

interface InmuebleRepository {
    suspend fun registrar(inmueble: Inmueble): AppResult<Inmueble>
    suspend fun obtenerTodos(): AppResult<List<Inmueble>>
    suspend fun obtenerPorCodigoPostal(codigoPostal: String): AppResult<List<Inmueble>>
    suspend fun obtenerPorDistrito(distrito: String): AppResult<List<Inmueble>>
    suspend fun actualizar(inmueble: Inmueble): AppResult<Inmueble>
    suspend fun darDeBaja(idInmueble: String): AppResult<Unit>
    suspend fun reactivarYActualizar(inmueble: Inmueble): AppResult<Inmueble>
}