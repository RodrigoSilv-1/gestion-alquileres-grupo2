package com.example.gestionalquileres.data.remote

import com.google.gson.annotations.SerializedName

data class ContratoDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("inmuebleId") val inmuebleId: String = "",
    @SerializedName("direccionInmueble") val direccionInmueble: String = "",
    @SerializedName("distritoInmueble") val distritoInmueble: String = "",
    @SerializedName("unidadId") val unidadId: String = "",
    @SerializedName("nombreUnidad") val nombreUnidad: String = "",
    @SerializedName("inquilinoId") val inquilinoId: String = "",
    @SerializedName("nombreInquilino") val nombreInquilino: String = "",
    @SerializedName("dniInquilino") val dniInquilino: String = "",
    @SerializedName("precioRentaAcordado") val precioRentaAcordado: Double = 0.0,
    @SerializedName("fechaInicio") val fechaInicio: String = "",
    @SerializedName("fechaFin") val fechaFin: String = "",
    @SerializedName("active") val active: Boolean = true
)