package com.example.gestionalquileres.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recibos")
data class ReciboEntity(
    @PrimaryKey val idRecibo: String,
    val nombreInquilino: String,
    val dniInquilino: String,
    val direccionInmueble: String,
    val distritoInmueble: String,
    val nombreUnidad: String,
    val numeroUnico: String,
    val idContrato: String,
    val fechaEmision: String,
    val rentaBase: Double,
    val montoIgv: Double,
    val agua: Double,
    val luz: Double,
    val mantenimiento: Double,
    val montoTotal: Double,
    val estadoPago: String,
    val syncStatus: String = "SYNCED"
)