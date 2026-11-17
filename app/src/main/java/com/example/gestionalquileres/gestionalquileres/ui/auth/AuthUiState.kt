package com.example.gestionalquileres.ui.auth

import com.example.gestionalquileres.domain.model.AppUser

data class AuthUiState(
    val isLoading: Boolean = false,
    val currentUser: AppUser? = null,
    val errorMessage: String? = null
)