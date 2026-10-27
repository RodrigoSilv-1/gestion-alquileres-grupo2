package com.example.gestionalquileres.ui.recibos

import com.example.gestionalquileres.domain.model.Contrato
import com.example.gestionalquileres.domain.model.Recibo

data class ReciboUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    val recibos: List<Recibo> = emptyList(),
    val contratosActivos: List<Contrato> = emptyList(),

    val mostrandoFormularioRegistro: Boolean = false,
    val reciboSeleccionado: Recibo? = null,

    val tipoBusqueda: String = "Número",
    val textoBusqueda: String = "",

    val filtroEstado: String = FILTRO_TODOS
) {
    // Bloque estático (companion object) que define las constantes para los tipos de filtro de recibos en la interfaz
    companion object {
        const val FILTRO_TODOS = "Todos"           // Muestra todos los recibos sin restricciones
        const val FILTRO_PENDIENTES = "Pendientes" // Muestra únicamente los recibos con estado pendiente
        const val FILTRO_COBRADOS = "Cobrados"     // Muestra únicamente los recibos con estado cobrado
    }

    // Propiedad calculada (custom getter) que devuelve una lista filtrada de recibos en tiempo real según el estado y la búsqueda
    val recibosFiltrados: List<Recibo>
        get() {
            // 1. Limpiamos los espacios en blanco al inicio y al final del texto ingresado en la barra de búsqueda
            val texto = textoBusqueda.trim()

            // 2. Filtramos la lista original de recibos evaluando dos condiciones principales por cada elemento
            return recibos.filter { recibo ->

                // A. Evaluamos si el recibo coincide con el filtro de estado seleccionado
                val coincideEstado = when (filtroEstado) {
                    FILTRO_PENDIENTES ->
                        recibo.estadoPago == Recibo.ESTADO_PENDIENTE

                    FILTRO_COBRADOS ->
                        recibo.estadoPago == Recibo.ESTADO_COBRADO

                    else -> true // Si el filtro es "Todos" (o cualquier otro valor por defecto), se aceptan todos
                }

                // B. Evaluamos si el recibo coincide con el texto de búsqueda (por número único o por ID de contrato)
                val coincideBusqueda =
                    texto.isBlank() || // Si la barra de búsqueda está vacía, coincide con todo
                            recibo.numeroUnico.contains(
                                other = texto,
                                ignoreCase = true // Ignora mayúsculas y minúsculas al buscar
                            ) ||
                            recibo.idContrato.contains(
                                other = texto,
                                ignoreCase = true // Ignora mayúsculas y minúsculas al buscar
                            )

                // 3. El recibo pasa el filtro solo si cumple tanto con la condición de estado como con la de búsqueda
                coincideEstado && coincideBusqueda
            }
        }
}