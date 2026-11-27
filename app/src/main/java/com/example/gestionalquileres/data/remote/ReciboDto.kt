package com.example.gestionalquileres.data.remote

import com.google.gson.annotations.SerializedName

data class ReciboDto(
    @SerializedName("id") val idRecibo: String? = null,
    @SerializedName("nombreInquilino") val nombreInquilino: String = "",
    @SerializedName("dniInquilino") val dniInquilino: String = "",
    @SerializedName("direccionInmueble") val direccionInmueble: String = "",
    @SerializedName("distritoInmueble") val distritoInmueble: String = "",
    @SerializedName("nombreUnidad") val nombreUnidad: String = "",
    @SerializedName("numeroUnico") val numeroUnico: String? = null,
    @SerializedName("idContrato") val idContrato: String? = null,
    @SerializedName("fechaEmision") val fechaEmision: String = "",
    @SerializedName("rentaBase") val rentaBase: Double = 0.0,
    @SerializedName("montoIgv") val montoIgv: Double = 0.0,
    @SerializedName("agua") val agua: Double = 0.0,
    @SerializedName("luz") val luz: Double = 0.0,
    @SerializedName("mantenimiento") val mantenimiento: Double = 0.0,
    @SerializedName("montoTotal") val montoTotal: Double = 0.0,
    @SerializedName("estadoPago") val estadoPago: String = "Pendiente"
)