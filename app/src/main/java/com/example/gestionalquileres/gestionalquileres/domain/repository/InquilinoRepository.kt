package com.example.gestionalquileres.domain.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Inquilino

interface InquilinoRepository {
    /** Solo inquilinos activos. */
    suspend fun obtenerTodos(): AppResult<List<Inquilino>>

    /** Busca por DNI entre activos e inactivos (para detectar inquilinos dados de baja). */
    suspend fun buscarPorDniCualquiera(dni: String): AppResult<Inquilino?>

    suspend fun registrar(inquilino: Inquilino): AppResult<Inquilino>
    suspend fun actualizar(inquilino: Inquilino): AppResult<Inquilino>
    suspend fun darDeBaja(idInquilino: String): AppResult<Unit>
    suspend fun reactivarYActualizar(inquilino: Inquilino): AppResult<Inquilino>
}