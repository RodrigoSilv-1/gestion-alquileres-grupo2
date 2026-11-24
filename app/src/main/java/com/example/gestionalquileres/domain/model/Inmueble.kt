package com.example.gestionalquileres.domain.model

import java.text.Normalizer
import com.google.firebase.firestore.Exclude

data class Inmueble(
    val idInmueble: String = "",
    val distrito: String = "",
    val direccion: String = "",
    val numeroDireccion: String = "",
    val codigoPostal: String = "",
    val numeroPisos: Int = 0,
    val unidadesPorPiso: Int = 0,
    /** MINIATURA en Base64 (≈160 px). La foto grande está en la colección "fotos". */
    val fotografia: String? = null,
    val active: Boolean = true,
    /** Foto grande recién elegida: solo viaja UI → ViewModel. NO se guarda en Firestore. */
    @get:Exclude val fotoCompleta: String? = null
)

/** Máximo de unidades (numeroPisos × unidadesPorPiso) que se generan en un único lote de Firestore. */
const val MAX_UNIDADES_POR_INMUEBLE = 400

/**
 * Clave que identifica un inmueble por su dirección. Ignora mayúsculas, tildes,
 * puntuación y espacios repetidos: "Av. Arequipa 123" y "av arequipa  123" son iguales.
 */
fun calcularClaveDireccion(distrito: String, direccion: String, numeroDireccion: String): String =
    listOf(distrito, direccion, numeroDireccion).joinToString("|") { it.normalizarParaClave() }

val Inmueble.claveDireccion: String
    get() = calcularClaveDireccion(distrito, direccion, numeroDireccion)

private fun String.normalizarParaClave(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")      // quita tildes
        .lowercase()
        .replace("[^a-z0-9]+".toRegex(), " ")   // puntuación y espacios repetidos → un espacio
        .trim()