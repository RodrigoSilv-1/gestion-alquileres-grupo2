package com.example.gestionalquileres.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.gestionalquileres.data.local.dao.InmuebleDao
import com.example.gestionalquileres.data.local.dao.InquilinoDao
import com.example.gestionalquileres.data.local.dao.PendingOperationDao
import com.example.gestionalquileres.data.local.dao.ReciboDao
import com.example.gestionalquileres.data.local.entity.InmuebleEntity
import com.example.gestionalquileres.data.local.entity.InquilinoEntity
import com.example.gestionalquileres.data.local.entity.PendingOperationEntity
import com.example.gestionalquileres.data.local.entity.ReciboEntity

@Database(
    entities = [
        InmuebleEntity::class,
        InquilinoEntity::class,
        ReciboEntity::class,
        PendingOperationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GestionAlquileresDatabase : RoomDatabase() {
    abstract fun inmuebleDao(): InmuebleDao
    abstract fun inquilinoDao(): InquilinoDao
    abstract fun reciboDao(): ReciboDao
    abstract fun pendingOperationDao(): PendingOperationDao
}