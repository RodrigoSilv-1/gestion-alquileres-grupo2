package com.example.gestionalquileres.domain.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Inquilino

interface InquilinoRepository {
    suspend fun registrar(inquilino: Inquilino): AppResult<Inquilino>
    suspend fun obtenerTodos(): AppResult<List<Inquilino>>
    suspend fun obtenerPorDni(dni: String): AppResult<List<Inquilino>>
    suspend fun obtenerPorNombre(nombre: String): AppResult<List<Inquilino>>
    suspend fun actualizar(inquilino: Inquilino): AppResult<Inquilino>
    suspend fun darDeBaja(idInquilino: String): AppResult<Unit>
    suspend fun buscarPorDniCualquiera(dni: String): AppResult<Inquilino?>
    suspend fun reactivarYActualizar(inquilino: Inquilino): AppResult<Inquilino>
}