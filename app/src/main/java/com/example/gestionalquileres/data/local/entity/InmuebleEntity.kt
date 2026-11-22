package com.example.gestionalquileres.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inmuebles")
data class InmuebleEntity(
    @PrimaryKey val id: String,
    val distrito: String,
    val direccion: String,
    val numeroDireccion: String,
    val codigoPostal: String,
    val numeroPisos: Int,
    val unidadesPorPiso: Int,
    val fotografia: String?,
    val active: Boolean,
    val syncStatus: String = "SYNCED"
)