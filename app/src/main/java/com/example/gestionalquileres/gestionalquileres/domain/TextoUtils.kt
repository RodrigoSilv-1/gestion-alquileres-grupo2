package com.example.gestionalquileres.domain

import java.text.Normalizer

/** Minúsculas, sin tildes ni espacios sobrantes: "  Miraflorés " -> "miraflores". Para comparar al buscar. */
fun String.normalizarBusqueda(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase()
        .trim()