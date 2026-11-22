package com.example.gestionalquileres.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gestionalquileres.data.local.entity.InquilinoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InquilinoDao {

    @Query("SELECT * FROM inquilinos WHERE active = 1")
    fun observeActivos(): Flow<List<InquilinoEntity>>

    @Query("SELECT * FROM inquilinos WHERE active = 1")
    suspend fun obtenerActivos(): List<InquilinoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(inquilino: InquilinoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(inquilinos: List<InquilinoEntity>)

    @Query("DELETE FROM inquilinos WHERE syncStatus = 'SYNCED' AND idInquilino NOT IN (:idsRemotos)")
    suspend fun eliminarObsoletosSincronizados(idsRemotos: List<String>)

    @Query("DELETE FROM inquilinos WHERE idInquilino = :id")
    suspend fun eliminarPorId(id: String)
}