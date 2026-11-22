package com.example.gestionalquileres.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inquilinos")
data class InquilinoEntity(
    @PrimaryKey val idInquilino: String,
    val nombre: String,
    val dni: String,
    val telefono: String,
    val edad: Int,
    val sexo: String,
    val fotografia: String,
    val active: Boolean = true,
    val sueldo: Double,
    val syncStatus: String = "SYNCED"
)