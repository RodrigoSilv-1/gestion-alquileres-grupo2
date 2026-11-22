package com.example.gestionalquileres.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "operaciones_pendientes")
data class PendingOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,   // "INMUEBLE", "INQUILINO", "RECIBO"
    val operationType: String, // "CREATE", "UPDATE"
    val entityId: String,
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis()
)