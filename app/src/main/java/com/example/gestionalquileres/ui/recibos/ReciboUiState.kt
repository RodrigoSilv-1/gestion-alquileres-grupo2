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
    //val reciboSeleccionado: Recibo? = null,

    //val tipoBusqueda: String = "Número",
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
        get() { //el get() va ejecutar esta fu
            // 1. Limpiamos los espacios en blanco al inicio y al final del texto ingresado en la barra de búsqueda
            val texto = textoBusqueda.trim()

            // 2. Filtramos la lista original de recibos evaluando dos condiciones principales por cada elemento
            return recibos.filter { recibo ->

                // A. Evaluamos si el recibo coincide con el filtro de estado seleccionado
                val coincideEstado = when (filtroEstado) {

                    // 1. Si el filtro activo es "Pendientes", el recibo solo pasa si su estado de pago es pendiente
                    FILTRO_PENDIENTES ->
                        recibo.estadoPago == Recibo.ESTADO_PENDIENTE

                    // 2. Si el filtro activo es "Cobrados", el recibo solo pasa si su estado de pago es cobrado
                    FILTRO_COBRADOS ->
                        recibo.estadoPago == Recibo.ESTADO_COBRADO

                    // 3. Caso por defecto (para "Todos" o cualquier otro valor): devuelve 'true',
                    // lo que significa que ningún recibo es descartado por el filtro de estado
                    else -> true
                }

                // B. Evaluamos si el recibo coincide con el texto de búsqueda (por número único o por ID de contrato)
                val coincideBusqueda =
                    texto.isBlank() || // 1. CASO VACÍO: Si la barra de búsqueda no tiene texto, permitimos que pase cualquier recibo.
                            recibo.numeroUnico.contains(
                                other = texto,
                                ignoreCase = true // 2. CASO NÚMERO: O si el número único del recibo incluye lo que escribiste (sin importar mayúsculas/minúsculas).
                            ) ||
                            recibo.idContrato.contains(
                                other = texto,
                                ignoreCase = true // 3. CASO CONTRATO: O si el ID de contrato también incluye lo que escribiste.
                            )

// El recibo pasa el filtro FINAL únicamente si cumple ambas condiciones al mismo tiempo (gracias al operador "&&"):
// 1. Debe coincidir con el estado seleccionado en las pestañas (Todos, Pendientes o Cobrados) -> 'coincideEstado'
// 2. Y también debe coincidir con el texto ingresado en la búsqueda -> 'coincideBusqueda'
                coincideEstado && coincideBusqueda
            }
        }
}