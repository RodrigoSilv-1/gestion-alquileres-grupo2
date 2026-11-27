package com.example.gestionalquileres.domain.model

import com.google.firebase.firestore.Exclude

data class Inquilino(
    val idInquilino: String = "",
    val nombre: String = "",
    val dni: String = "",
    val telefono: String = "",
    val edad: Int = 0,
    val sexo: String = "",
    /** MINIATURA en Base64 (≈160 px). La foto grande está en la colección "fotos". */
    val fotografia: String? = null,
    val active: Boolean = true,
    val sueldo: Double = 0.0,
    /** Foto grande recién elegida: solo viaja UI → ViewModel. NO se guarda en Firestore. */
    @get:Exclude val fotoCompleta: String? = null
)