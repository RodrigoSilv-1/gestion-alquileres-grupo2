package com.example.gestionalquileres.di

import com.example.gestionalquileres.data.auth.FirebaseAuthRepository
import com.example.gestionalquileres.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Enlaza la interfaz de autenticación (domain) con su implementación Firebase (data). */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository
}
