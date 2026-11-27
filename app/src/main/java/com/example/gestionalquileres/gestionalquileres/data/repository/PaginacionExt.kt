package com.example.gestionalquileres.data.repository

import com.example.gestionalquileres.domain.model.CursorPagina
import com.example.gestionalquileres.domain.model.Pagina
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

internal class CursorFirestore(val ultimo: DocumentSnapshot) : CursorPagina

/** Lee una página con limit + cursor (startAfter). La consulta debe tener su orderBy. */
internal suspend fun <T : Any> Query.leerPagina(
    limite: Int,
    despues: CursorPagina?,
    mapear: (DocumentSnapshot) -> T?
): Pagina<T> {
    require(limite > 0) { "El tamaño de página debe ser mayor que cero." }

    val inicio = (despues as? CursorFirestore)?.let { startAfter(it.ultimo) } ?: this

    // Se pide uno de más para saber si hay otra página sin hacer una consulta vacía extra
    val documentos = inicio.limit(limite + 1L).get().await().documents
    val hayMas = documentos.size > limite
    val dePagina = if (hayMas) documentos.take(limite) else documentos

    return Pagina(
        items = dePagina.mapNotNull(mapear),
        siguiente = if (hayMas) CursorFirestore(dePagina.last()) else null
    )
}