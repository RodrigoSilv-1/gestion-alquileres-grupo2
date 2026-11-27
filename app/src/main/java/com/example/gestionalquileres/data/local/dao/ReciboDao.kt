package com.example.gestionalquileres.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gestionalquileres.data.local.entity.ReciboEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReciboDao {

    @Query("SELECT * FROM recibos")
    fun observeTodos(): Flow<List<ReciboEntity>>

    @Query("SELECT * FROM recibos")
    suspend fun obtenerTodos(): List<ReciboEntity>

    @Query("SELECT * FROM recibos WHERE idRecibo = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): ReciboEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(recibo: ReciboEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(recibos: List<ReciboEntity>)

    @Query("DELETE FROM recibos WHERE syncStatus = 'SYNCED' AND idRecibo NOT IN (:idsRemotos)")
    suspend fun eliminarObsoletosSincronizados(idsRemotos: List<String>)

    @Query("DELETE FROM recibos WHERE idRecibo = :id")
    suspend fun eliminarPorId(id: String)

}