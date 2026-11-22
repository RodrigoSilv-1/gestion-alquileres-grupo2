package com.example.gestionalquileres.data.remote

import com.google.gson.annotations.SerializedName

data class InmuebleDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("direccion") val direccion: String = "",
    @SerializedName("unidadesPorPiso") val unidadesPorPiso: Int = 0,
    @SerializedName("distrito") val distrito: String = "",
    @SerializedName("numeroDireccion") val numeroDireccion: String = "",
    @SerializedName("codigoPostal") val codigoPostal: String = "",
    @SerializedName("numeroPisos") val numeroPisos: Int = 0,
    @SerializedName("fotografia") val fotografia: String = "",
    @SerializedName("active") val active: Boolean = true
)