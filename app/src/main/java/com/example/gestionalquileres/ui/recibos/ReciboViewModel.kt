package com.example.gestionalquileres.ui.recibos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionalquileres.data.repository.ContratoRepositoryImpl
import com.example.gestionalquileres.data.repository.ReciboRepositoryImpl
import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Contrato
import com.example.gestionalquileres.domain.model.Recibo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import com.example.gestionalquileres.domain.model.onFailure
import com.example.gestionalquileres.domain.model.onSuccess

@HiltViewModel
class ReciboViewModel @Inject constructor(
    private val reciboRepository: ReciboRepositoryImpl,
    private val contratoRepository: ContratoRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReciboUiState())
    val uiState: StateFlow<ReciboUiState> = _uiState.asStateFlow()

    fun cargarContratosParaCobro() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            contratoRepository.obtenerContratosActivos()
                .onSuccess { contratos ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            contratosActivos = contratos
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "No se pudieron cargar los contratos."
                        )
                    }
                }
        }
    }

    fun generarRecibo(
        contratoSeleccionado: Contrato,
        inputAgua: String,
        inputLuz: String,
        inputMantenimiento: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val agua = inputAgua.toDoubleOrNull() ?: 0.0
            val luz = inputLuz.toDoubleOrNull() ?: 0.0
            val mantenimiento = inputMantenimiento.toDoubleOrNull() ?: 0.0
            val renta = contratoSeleccionado.precioRentaAcordado

            val igv = renta * 0.18
            val total = renta + igv + agua + luz + mantenimiento

            val formatoFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val fechaHoy = formatoFecha.format(Date())

            val reciboParcial = Recibo(
                idContrato = contratoSeleccionado.idContrato,
                nombreInquilino = contratoSeleccionado.nombreInquilino,
                dniInquilino = contratoSeleccionado.dniInquilino,
                direccionInmueble = contratoSeleccionado.direccionInmueble,
                distritoInmueble = contratoSeleccionado.distritoInmueble,
                nombreUnidad = contratoSeleccionado.nombreUnidad,
                fechaEmision = fechaHoy,
                rentaBase = renta,
                montoIgv = igv,
                agua = agua,
                luz = luz,
                mantenimiento = mantenimiento,
                montoTotal = total,
                estadoPago = Recibo.ESTADO_PENDIENTE
            )

            reciboRepository.generarYGuardarRecibo(reciboParcial)
                .onSuccess { creado ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            recibos = (listOf(creado) + it.recibos).distinctBy { item -> item.idRecibo },
                            successMessage = "Recibo ${creado.numeroUnico} generado correctamente."
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Error al generar recibo."
                        )
                    }
                }
        }
    }

    fun cargarHistorialRecibos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            reciboRepository.obtenerTodosLosRecibos()
                .onSuccess { lista ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            recibos = lista.distinctBy { item -> item.idRecibo }
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Error al cargar historial."
                        )
                    }
                }
        }
    }

    fun cambiarEstadoPago(recibo: Recibo) {
        val nuevoEstado =
            if (recibo.estadoPago == Recibo.ESTADO_COBRADO) {
                Recibo.ESTADO_PENDIENTE
            } else {
                Recibo.ESTADO_COBRADO
            }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    successMessage = null
                )
            }

            reciboRepository.actualizarEstadoPago(
                idRecibo = recibo.idRecibo,
                nuevoEstado = nuevoEstado
            )
                .onSuccess {
                    _uiState.update { estado ->
                        estado.copy(
                            isLoading = false,
                            recibos = estado.recibos.map { actual ->
                                if (actual.idRecibo == recibo.idRecibo) {
                                    actual.copy(estadoPago = nuevoEstado)
                                } else {
                                    actual
                                }
                            },
                            successMessage = "Recibo marcado como $nuevoEstado."
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message
                                ?: "No se pudo actualizar el estado del recibo."
                        )
                    }
                }
        }
    }

    fun mostrarFormularioRegistro(mostrar: Boolean) {
        _uiState.update { it.copy(mostrandoFormularioRegistro = mostrar) }
    }

    fun actualizarTextoBusqueda(texto: String) {
        _uiState.update { it.copy(textoBusqueda = texto) }
    }

    fun actualizarFiltroEstado(filtro: String) {
        val filtrosPermitidos = setOf(
            ReciboUiState.FILTRO_TODOS,
            ReciboUiState.FILTRO_PENDIENTES,
            ReciboUiState.FILTRO_COBRADOS
        )

        if (filtro in filtrosPermitidos) {
            _uiState.update {
                it.copy(filtroEstado = filtro)
            }
        }
    }

    fun limpiarMensajes() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                successMessage = null
            )
        }
    }
}