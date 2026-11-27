package com.example.gestionalquileres.data.remote

import com.google.gson.annotations.SerializedName

data class InquilinoDto(
    @SerializedName("id") val idInquilino: String? = null,
    @SerializedName("nombre") val nombre: String = "",
    @SerializedName("dni") val dni: String = "",
    @SerializedName("telefono") val telefono: String = "",
    @SerializedName("edad") val edad: Int = 0,
    @SerializedName("sexo") val sexo: String = "",
    @SerializedName("fotografia") val fotografia: String? = null,
    @SerializedName("active") val active: Boolean = true,
    @SerializedName("sueldo") val sueldo: Double = 0.0
)