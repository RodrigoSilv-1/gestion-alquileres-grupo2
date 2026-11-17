package com.example.gestionalquileres.domain.model

data class AppUser(
    val uid: String = "",
    val email: String = "",
    val role: String = UserRole.SECRETARIO.name,
    val active: Boolean = true
)