package com.example.gestionalquileres.ui.recibos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionalquileres.domain.calcularIgv
import com.example.gestionalquileres.domain.comoMontoOCero
import com.example.gestionalquileres.domain.model.Contrato
import com.example.gestionalquileres.domain.model.EstadoPago
import com.example.gestionalquileres.domain.model.FiltroRecibos
import com.example.gestionalquileres.domain.model.Recibo
import com.example.gestionalquileres.domain.model.onFailure
import com.example.gestionalquileres.domain.model.onSuccess
import com.example.gestionalquileres.domain.redondearMonto
import com.example.gestionalquileres.domain.repository.ContratoRepository
import com.example.gestionalquileres.domain.repository.ReciboRepository
import com.example.gestionalquileres.domain.sumarMontos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ReciboViewModel @Inject constructor(
    private val reciboRepository: ReciboRepository,
    private val contratoRepository: ContratoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReciboUiState())
    val uiState: StateFlow<ReciboUiState> = _uiState.asStateFlow()

    // ---------- Helpers privados ----------

    private fun iniciarOperacion() =
        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

    private fun fallo(error: Exception, mensajePorDefecto: String) =
        _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: mensajePorDefecto) }

    /** Error de validación: no hubo operación en curso, así que no toca isLoading. */
    private fun mostrarError(mensaje: String) =
        _uiState.update { it.copy(errorMessage = mensaje) }

    // ---------- Navegación interna ----------

    fun mostrarFormularioRegistro(mostrar: Boolean) =
        _uiState.update { it.copy(mostrandoFormularioRegistro = mostrar) }

    fun volverAlListado() =
        _uiState.update { it.copy(mostrandoFormularioRegistro = false) }

    fun limpiarMensajes() =
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }

    // ---------- Búsqueda ----------

    fun actualizarTextoBusqueda(texto: String) =
        _uiState.update { it.copy(textoBusqueda = texto) }

    // ---------- Contratos (formulario y filtro) ----------

    /** Carga los contratos vigentes una sola vez; el formulario y el filtro comparten la lista. */
    fun cargarContratos() {
        if (_uiState.value.contratosActivos.isNotEmpty()) return
        viewModelScope.launch {
            contratoRepository.obtenerContratosActivos()
                .onSuccess { contratos -> _uiState.update { it.copy(contratosActivos = contratos) } }
                .onFailure { error ->
                    mostrarError(error.message ?: "No se pudieron cargar los contratos.")
                }
        }
    }

    // ---------- Generar recibo ----------

    fun generarRecibo(
        contrato: Contrato,
        inputAgua: String,
        inputLuz: String,
        inputIpc: String
    ) {
        viewModelScope.launch {
            iniciarOperacion()

            // Campos vacíos o inválidos se vuelven 0.0; acepta "12,5" y "12.5"
            val agua = inputAgua.comoMontoOCero()
            val luz = inputLuz.comoMontoOCero()
            val ipc = inputIpc.comoMontoOCero()
            val renta = contrato.precioRentaAcordado.redondearMonto()

            // Cálculos con BigDecimal, siempre a 2 decimales
            val igv = calcularIgv(renta)
            val total = sumarMontos(renta, igv, agua, luz, ipc)

            val reciboParcial = Recibo(
                idContrato = contrato.idContrato,
                nombreInquilino = contrato.nombreInquilino,
                dniInquilino = contrato.dniInquilino,
                direccionInmueble = contrato.direccionInmueble,
                distritoInmueble = contrato.distritoInmueble,
                nombreUnidad = contrato.nombreUnidad,
                fechaEmision = LocalDate.now().toString(), // yyyy-MM-dd, sin depender del idioma del teléfono
                rentaBase = renta,
                montoIgv = igv,
                agua = agua,
                luz = luz,
                ipc = ipc,
                montoTotal = total,
                estadoPago = EstadoPago.PENDIENTE.valor
            )

            // El repositorio crea el correlativo y guarda
            reciboRepository.generarYGuardarRecibo(reciboParcial)
                .onSuccess { generado ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            mostrandoFormularioRegistro = false,
                            successMessage = "Recibo ${generado.numeroUnico} generado correctamente."
                        )
                    }
                    cargarHistorialRecibos()
                }
                .onFailure { error -> fallo(error, "Error al generar recibo.") }
        }
    }

    // ---------- Historial paginado ----------

    /** Primera página del historial con el filtro actual. */
    fun cargarHistorialRecibos() {
        val filtro = _uiState.value.filtro
        viewModelScope.launch {
            // No usa iniciarOperacion(): se llama tras generar un recibo y no debe borrar el mensaje de éxito
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            reciboRepository.obtenerPagina(filtro)
                .onSuccess { pagina ->
                    _uiState.update {
                        it.copy(isLoading = false, recibos = pagina.items, siguientePagina = pagina.siguiente)
                    }
                }
                .onFailure { error -> fallo(error, "Error al cargar historial.") }
        }
    }

    fun cargarMasRecibos() {
        val cursor = _uiState.value.siguientePagina ?: return
        if (_uiState.value.cargandoMas) return
        val filtro = _uiState.value.filtro
        _uiState.update { it.copy(cargandoMas = true) }

        viewModelScope.launch {
            reciboRepository.obtenerPagina(filtro, despues = cursor)
                .onSuccess { pagina ->
                    _uiState.update { estado ->
                        estado.copy(
                            cargandoMas = false,
                            recibos = (estado.recibos + pagina.items).distinctBy { it.idRecibo },
                            siguientePagina = pagina.siguiente
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(cargandoMas = false, errorMessage = error.message ?: "No se pudieron cargar más recibos.")
                    }
                }
        }
    }

    // ---------- Filtros ----------

    private fun aplicarFiltro(nuevo: FiltroRecibos) {
        _uiState.update { it.copy(filtro = nuevo, recibos = emptyList(), siguientePagina = null) }
        cargarHistorialRecibos()
    }

    fun filtrarPorContrato(idContrato: String?) =
        aplicarFiltro(_uiState.value.filtro.copy(idContrato = idContrato, numero = null))

    fun filtrarPorFechas(desde: String?, hasta: String?) {
        val d = desde?.takeIf { it.isNotBlank() }
        val h = hasta?.takeIf { it.isNotBlank() }
        if (d != null && h != null && d > h) {
            mostrarError("La fecha \"Desde\" no puede ser posterior a \"Hasta\".")
            return
        }
        aplicarFiltro(_uiState.value.filtro.copy(desde = d, hasta = h, numero = null))
    }

    fun buscarPorNumero() {
        val numero = _uiState.value.textoBusqueda.trim()
        aplicarFiltro(FiltroRecibos(numero = numero.ifBlank { null }))
    }

    fun filtrarPorEstado(estado: EstadoPago?) =
        aplicarFiltro(_uiState.value.filtro.copy(estado = estado, numero = null))

    fun limpiarFiltros() {
        _uiState.update { it.copy(textoBusqueda = "") }
        aplicarFiltro(FiltroRecibos())
    }

    // ---------- Control de cobros ----------

    /** Alterna el recibo entre Pendiente y Pagado. */
    fun cambiarEstadoPago(recibo: Recibo) {
        if (_uiState.value.idReciboActualizando != null) return
        val nuevoEstado =
            if (recibo.estado() == EstadoPago.PAGADO) EstadoPago.PENDIENTE else EstadoPago.PAGADO

        // No usa isLoading: taparía la lista con el spinner
        _uiState.update { it.copy(idReciboActualizando = recibo.idRecibo, errorMessage = null) }

        viewModelScope.launch {
            reciboRepository.actualizarEstadoPago(recibo.idRecibo, nuevoEstado)
                .onSuccess {
                    _uiState.update { estado ->
                        val filtroEstado = estado.filtro.estado
                        val actualizados = estado.recibos.mapNotNull { actual ->
                            when {
                                actual.idRecibo != recibo.idRecibo -> actual
                                // Si hay filtro por estado y ya no coincide, sale de la lista
                                filtroEstado != null && filtroEstado != nuevoEstado -> null
                                else -> actual.copy(estadoPago = nuevoEstado.valor)
                            }
                        }
                        estado.copy(idReciboActualizando = null, recibos = actualizados)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            idReciboActualizando = null,
                            errorMessage = error.message ?: "No se pudo actualizar el estado del recibo."
                        )
                    }
                }
        }
    }
}