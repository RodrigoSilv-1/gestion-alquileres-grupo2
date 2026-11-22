package com.example.gestionalquileres.data.remote

import com.google.gson.annotations.SerializedName

data class UnidadAlquilableDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("inmuebleId") val inmuebleId: String = "",
    @SerializedName("piso") val piso: Int = 0,
    @SerializedName("numeroUnidad") val numeroUnidad: Int = 0,
    @SerializedName("nombreFormateado") val nombreFormateado: String = "",
    @SerializedName("estaOcupada") val estaOcupada: Boolean = false,
    @SerializedName("precioRenta") val precioRenta: Double = 0.0
)