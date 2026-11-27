package com.example.gestionalquileres.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionalquileres.domain.model.onFailure
import com.example.gestionalquileres.domain.model.onSuccess
import com.example.gestionalquileres.domain.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var jobCobranza: Job? = null

    init { recargar() }

    fun recargar() {
        cargarResumen()
        cargarCobranza()
    }

    /** Mueve el mes de la cobranza (-1 anterior, +1 siguiente). No permite meses futuros. */
    fun cambiarMes(delta: Long) {
        val nuevo = _uiState.value.mes.plusMonths(delta)
        if (nuevo.isAfter(YearMonth.now())) return
        _uiState.update { it.copy(mes = nuevo) }
        cargarCobranza()
    }

    fun limpiarError() = _uiState.update { it.copy(errorMessage = null) }

    private fun cargarResumen() {
        val mes = YearMonth.now()
        _uiState.update { it.copy(cargandoResumen = true) }
        viewModelScope.launch {
            dashboardRepository.obtenerResumenGeneral(mes.atDay(1).toString(), mes.atEndOfMonth().toString())
                .onSuccess { resumen ->
                    _uiState.update { it.copy(resumen = resumen, cargandoResumen = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(cargandoResumen = false, errorMessage = error.message ?: "No se pudo cargar el resumen.")
                    }
                }
        }
    }

    private fun cargarCobranza() {
        jobCobranza?.cancel() // si el usuario cambia de mes rápido, solo vale la última carga
        val mes = _uiState.value.mes
        _uiState.update { it.copy(cargandoCobranza = true) }
        jobCobranza = viewModelScope.launch {
            dashboardRepository.obtenerCobranzaMes(mes.atDay(1).toString(), mes.atEndOfMonth().toString())
                .onSuccess { cobranza ->
                    _uiState.update { it.copy(cobranza = cobranza, cargandoCobranza = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(cargandoCobranza = false, errorMessage = error.message ?: "No se pudo cargar la cobranza.")
                    }
                }
        }
    }
}