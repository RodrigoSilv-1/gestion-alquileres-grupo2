package com.example.gestionalquileres.ui.recibos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionalquileres.domain.repository.ContratoRepository
import com.example.gestionalquileres.domain.repository.ReciboRepository
import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Contrato
import com.example.gestionalquileres.domain.model.Recibo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.gestionalquileres.domain.calcularIgv
import com.example.gestionalquileres.domain.comoMontoOCero
import com.example.gestionalquileres.domain.model.EstadoPago
import com.example.gestionalquileres.domain.redondearMonto
import com.example.gestionalquileres.domain.sumarMontos
import java.time.LocalDate
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

            // 1. Campos vacíos o inválidos se vuelven 0.0; acepta "12,5" y "12.5"
            val agua = inputAgua.comoMontoOCero()
            val luz = inputLuz.comoMontoOCero()
            val ipc = inputIpc.comoMontoOCero()
            val renta = contratoSeleccionado.precioRentaAcordado.redondearMonto()

            // 2. Cálculos con BigDecimal, siempre a 2 decimales
            val igv = calcularIgv(renta)
            val total = sumarMontos(renta, igv, agua, luz, ipc)

            // 3. Fecha del sistema en formato yyyy-MM-dd (igual que antes, sin depender del idioma del teléfono)
            val fechaHoy = LocalDate.now().toString()

            val reciboParcial = Recibo(
                idContrato = contratoSeleccionado.idContrato,
                fechaEmision = fechaHoy,
                rentaBase = renta,
                montoIgv = igv,
                agua = agua,
                luz = luz,
                ipc = ipc,
                montoTotal = total,
                estadoPago = EstadoPago.PENDIENTE.valor
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