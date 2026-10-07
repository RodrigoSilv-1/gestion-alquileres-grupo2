package com.example.gestionalquileres.data.auth

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.AppUser

interface AuthRepository {

    suspend fun register(
        email: String,
        password: String
    ): AppResult<AppUser>

    suspend fun login(
        email: String,
        password: String
    ): AppResult<AppUser>

    suspend fun getCurrentUser(): AppResult<AppUser>

    fun logout()

    fun getCurrentUserId(): String?
}