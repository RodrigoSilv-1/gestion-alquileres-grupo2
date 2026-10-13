package com.example.gestionalquileres.ui.recibos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionalquileres.data.repository.ContratoRepository
import com.example.gestionalquileres.data.repository.ReciboRepository
import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Contrato
import com.example.gestionalquileres.domain.model.Recibo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ReciboViewModel @Inject constructor(
    private val reciboRepository: ReciboRepository,
    private val contratoRepository: ContratoRepository // Inyectado para traer los contratos activos
) : ViewModel() {

    // (Aquí iría tu StateFlow tradicional, resumido para el ejemplo)
    private val _uiState = MutableStateFlow(ReciboUiState())
    val uiState: StateFlow<ReciboUiState> = _uiState.asStateFlow()

    fun cargarContratosParaCobro() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // Proceso 1: Buscar a quién cobrar
            when (val resultado = contratoRepository.obtenerContratosActivos()) {
                is AppResult.Exito -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        contratosActivos = resultado.valor
                    )
                }
                is AppResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No se pudieron cargar los contratos."
                    )
                }
            }
        }
    }

    // Proceso 4 y 5: Procesar datos del formulario y enviar a Firestore
    fun generarRecibo(
        contratoSeleccionado: Contrato,
        inputAgua: String,
        inputLuz: String,
        inputIpc: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // 1. Manejo seguro de campos vacíos (se vuelven 0.0) - Criterio 5
            val agua = inputAgua.toDoubleOrNull() ?: 0.0
            val luz = inputLuz.toDoubleOrNull() ?: 0.0
            val ipc = inputIpc.toDoubleOrNull() ?: 0.0
            val renta = contratoSeleccionado.precioRentaAcordado

            // 2. Cálculos (Criterio 4)
            val igv = renta * 0.18
            val total = renta + igv + agua + luz + ipc

            // 3. Capturar fecha del sistema (Criterio 3)
            val formatoFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val fechaHoy = formatoFecha.format(Date())

            val reciboParcial = Recibo(
                idContrato = contratoSeleccionado.idContrato,
                fechaEmision = fechaHoy,
                rentaBase = renta,
                montoIgv = igv,
                agua = agua,
                luz = luz,
                ipc = ipc,
                montoTotal = total,
                estadoPago = "Pendiente"
            )

            // 4. Delegar al repositorio para crear el correlativo y guardar
            when (val resultado = reciboRepository.generarYGuardarRecibo(reciboParcial)) {
                is AppResult.Exito -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Recibo ${resultado.valor.numeroUnico} generado correctamente."
                    )
                }
                is AppResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = resultado.excepcion.message ?: "Error al generar recibo."
                    )
                }
            }
        }
    }

    fun cargarHistorialRecibos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            when (val resultado = reciboRepository.obtenerTodosLosRecibos()) {
                is AppResult.Exito -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        recibos = resultado.valor // Recuerda cambiar "data" si tu AppResult usa otro nombre
                    )
                }
                is AppResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = resultado.excepcion.message ?: "Error al cargar historial."
                    )
                }
            }
        }
    }

    // --- FUNCIONES DE ESTADO DE LA INTERFAZ (UI STATE) ---

    fun mostrarFormularioRegistro(mostrar: Boolean) {
        _uiState.value = _uiState.value.copy(mostrandoFormularioRegistro = mostrar)
    }

    fun seleccionarRecibo(recibo: Recibo?) {
        _uiState.value = _uiState.value.copy(reciboSeleccionado = recibo)
    }

    fun actualizarTipoBusqueda(tipo: String) {
        _uiState.value = _uiState.value.copy(tipoBusqueda = tipo)
    }

    fun actualizarTextoBusqueda(texto: String) {
        _uiState.value = _uiState.value.copy(textoBusqueda = texto)
    }

    fun limpiarMensajes() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}