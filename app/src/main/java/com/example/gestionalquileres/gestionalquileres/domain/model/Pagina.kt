package com.example.gestionalquileres.domain.model

const val TAMANO_PAGINA = 20

/** Marcador opaco de "hasta dónde llegó la lectura". Solo la capa data sabe qué contiene. */
interface CursorPagina

/** Una página de resultados. [siguiente] es null cuando ya no hay más. */
data class Pagina<T>(
    val items: List<T>,
    val siguiente: CursorPagina?
) {
    val hayMas: Boolean get() = siguiente != null
}