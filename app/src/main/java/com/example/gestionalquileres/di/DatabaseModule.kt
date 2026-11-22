package com.example.gestionalquileres.di

import android.content.Context
import androidx.room.Room
import com.example.gestionalquileres.data.local.GestionAlquileresDatabase
import com.example.gestionalquileres.data.local.dao.InmuebleDao
import com.example.gestionalquileres.data.local.dao.InquilinoDao
import com.example.gestionalquileres.data.local.dao.PendingOperationDao
import com.example.gestionalquileres.data.local.dao.ReciboDao
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GestionAlquileresDatabase {
        return Room.databaseBuilder(
            context,
            GestionAlquileresDatabase::class.java,
            "gestion_alquileres.db"
        ).build()
    }

    @Provides
    fun provideInmuebleDao(db: GestionAlquileresDatabase): InmuebleDao = db.inmuebleDao()

    @Provides
    fun provideInquilinoDao(db: GestionAlquileresDatabase): InquilinoDao = db.inquilinoDao()

    @Provides
    fun provideReciboDao(db: GestionAlquileresDatabase): ReciboDao = db.reciboDao()

    @Provides
    fun providePendingOperationDao(db: GestionAlquileresDatabase): PendingOperationDao =
        db.pendingOperationDao()

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()
}