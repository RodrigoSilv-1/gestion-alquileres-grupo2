package com.example.gestionalquileres.di

import com.example.gestionalquileres.data.repository.FirestoreContratoRepository
import com.example.gestionalquileres.data.repository.FirestoreInmuebleRepository
import com.example.gestionalquileres.data.repository.FirestoreInquilinoRepository
import com.example.gestionalquileres.data.repository.FirestoreReciboRepository
import com.example.gestionalquileres.data.repository.FirestoreUnidadAlquilableRepository
import com.example.gestionalquileres.domain.repository.ContratoRepository
import com.example.gestionalquileres.domain.repository.InmuebleRepository
import com.example.gestionalquileres.domain.repository.InquilinoRepository
import com.example.gestionalquileres.domain.repository.ReciboRepository
import com.example.gestionalquileres.domain.repository.UnidadAlquilableRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Enlaza cada interfaz de repositorio (domain) con su implementación Firestore (data). */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindInquilinoRepository(impl: FirestoreInquilinoRepository): InquilinoRepository

    @Binds
    abstract fun bindInmuebleRepository(impl: FirestoreInmuebleRepository): InmuebleRepository

    @Binds
    abstract fun bindUnidadAlquilableRepository(impl: FirestoreUnidadAlquilableRepository): UnidadAlquilableRepository

    @Binds
    abstract fun bindContratoRepository(impl: FirestoreContratoRepository): ContratoRepository

    @Binds
    abstract fun bindReciboRepository(impl: FirestoreReciboRepository): ReciboRepository
}