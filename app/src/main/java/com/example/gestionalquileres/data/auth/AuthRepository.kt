package com.example.gestionalquileres.data.auth

import com.example.gestionalquileres.domain.model.AppUser
import com.example.gestionalquileres.domain.model.UserRole


interface AuthRepository {

    suspend fun register(
        email: String,
        password: String,
        role: UserRole
    ): Result<AppUser>

    suspend fun login(
        email: String,
        password: String
    ): Result<AppUser>

    suspend fun signInWithGoogle(
        idToken: String,
        role: UserRole
    ): Result<AppUser>
    suspend fun getCurrentUser(): Result<AppUser>

    fun logout()

    fun getCurrentUserId(): String?
}