package com.example.gestionalquileres.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gestionalquileres.data.local.entity.InmuebleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InmuebleDao {

    @Query("SELECT * FROM inmuebles WHERE active = 1")
    fun observeActivos(): Flow<List<InmuebleEntity>>

    @Query("SELECT * FROM inmuebles WHERE active = 1")
    suspend fun obtenerActivos(): List<InmuebleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(inmueble: InmuebleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(inmuebles: List<InmuebleEntity>)

    @Query("DELETE FROM inmuebles WHERE syncStatus = 'SYNCED' AND id NOT IN (:idsRemotos)")
    suspend fun eliminarObsoletosSincronizados(idsRemotos: List<String>)

    @Query("DELETE FROM inmuebles WHERE id = :id")
    suspend fun eliminarPorId(id: String)
}