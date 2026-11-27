package com.example.gestionalquileres.di

import com.example.gestionalquileres.data.repository.ContratoRepositoryImpl
import com.example.gestionalquileres.data.repository.DashboardRepositoryImpl
import com.example.gestionalquileres.data.repository.FotoRepositoryImpl
import com.example.gestionalquileres.data.repository.InmuebleRepositoryImpl
import com.example.gestionalquileres.data.repository.InquilinoRepositoryImpl
import com.example.gestionalquileres.data.repository.ReciboRepositoryImpl
import com.example.gestionalquileres.data.repository.UnidadAlquilableRepositoryImpl
import com.example.gestionalquileres.domain.model.FotoRepository
import com.example.gestionalquileres.domain.repository.ContratoRepository
import com.example.gestionalquileres.domain.repository.DashboardRepository
import com.example.gestionalquileres.domain.repository.InmuebleRepository
import com.example.gestionalquileres.domain.repository.InquilinoRepository
import com.example.gestionalquileres.domain.repository.ReciboRepository
import com.example.gestionalquileres.domain.repository.UnidadAlquilableRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindInquilinoRepository(
        impl: InquilinoRepositoryImpl
    ): InquilinoRepository

    @Binds
    @Singleton
    abstract fun bindInmuebleRepository(
        impl: InmuebleRepositoryImpl
    ): InmuebleRepository

    @Binds
    @Singleton
    abstract fun bindContratoRepository(
        impl: ContratoRepositoryImpl
    ): ContratoRepository

    @Binds
    @Singleton
    abstract fun bindReciboRepository(
        impl: ReciboRepositoryImpl
    ): ReciboRepository

    @Binds
    @Singleton
    abstract fun bindUnidadAlquilableRepository(
        impl: UnidadAlquilableRepositoryImpl
    ): UnidadAlquilableRepository

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(
        impl: DashboardRepositoryImpl
    ): DashboardRepository

    @Binds
    @Singleton
    abstract fun bindFotoRepository(
        impl: FotoRepositoryImpl
    ): FotoRepository
}