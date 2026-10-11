package com.example.gestionalquileres.ui.recibos

import com.example.gestionalquileres.domain.model.Contrato
import com.example.gestionalquileres.domain.model.Recibo

data class ReciboUiState(
    // Estados generales
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // Datos principales
    val recibos: List<Recibo> = emptyList(), // Historial de recibos generados
    val contratosActivos: List<Contrato> = emptyList(), // Para el dropdown de "A quién cobrar"

    // Controles de navegación y formularios
    val mostrandoFormularioRegistro: Boolean = false,
    val reciboSeleccionado: Recibo? = null,

    // Búsqueda y filtros (Estándar de tu app)
    val tipoBusqueda: String = "Número", // Puede ser "Número" o "Contrato"
    val textoBusqueda: String = ""
)
