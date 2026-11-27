package com.example.gestionalquileres.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.gestionalquileres.data.local.entity.PendingOperationEntity

@Dao
interface PendingOperationDao {

    @Query("SELECT * FROM operaciones_pendientes ORDER BY createdAt ASC, id ASC")
    suspend fun obtenerPendientesOrdenados(): List<PendingOperationEntity>

    @Insert
    suspend fun encolar(operacion: PendingOperationEntity)

    @Delete
    suspend fun eliminar(operacion: PendingOperationEntity)
}