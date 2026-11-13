package com.example.gestionalquileres.domain.model

/** Estados de un recibo. [valor] es el texto que se guarda en Firestore (no cambia respecto a lo ya guardado). */
enum class EstadoPago(val valor: String) {
    PENDIENTE("Pendiente"),
    PAGADO("Pagado");

    companion object {
        /** Convierte el texto guardado en su estado; si no se reconoce, se considera pendiente. */
        fun desde(texto: String): EstadoPago =
            values().firstOrNull { it.valor.equals(texto.trim(), ignoreCase = true) } ?: PENDIENTE
    }
}