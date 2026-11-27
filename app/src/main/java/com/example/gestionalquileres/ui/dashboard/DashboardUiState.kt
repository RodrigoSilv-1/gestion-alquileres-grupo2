package com.example.gestionalquileres.ui.dashboard

import com.example.gestionalquileres.domain.model.CobranzaMes
import com.example.gestionalquileres.domain.model.ResumenGeneral
import com.example.gestionalquileres.domain.model.IngresoMensual
import java.time.YearMonth

data class DashboardUiState(
    val mes: YearMonth = YearMonth.now(),          // mes que muestra la cobranza
    val resumen: ResumenGeneral? = null,
    val cobranza: CobranzaMes? = null,
    val ingresos: List<IngresoMensual> = emptyList(),
    val cargandoIngresos: Boolean = true,
    val cargandoResumen: Boolean = true,
    val cargandoCobranza: Boolean = true,
    val errorMessage: String? = null
) {
    val esMesActual: Boolean get() = mes >= YearMonth.now()
}