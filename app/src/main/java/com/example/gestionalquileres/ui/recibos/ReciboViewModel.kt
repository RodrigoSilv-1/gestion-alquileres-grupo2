package com.example.gestionalquileres.ui.recibos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionalquileres.data.repository.ContratoRepositoryImpl
import com.example.gestionalquileres.data.repository.ReciboRepositoryImpl
import com.example.gestionalquileres.domain.model.Contrato
import com.example.gestionalquileres.domain.model.EstadoPago
import com.example.gestionalquileres.domain.model.FiltroRecibos
import com.example.gestionalquileres.domain.model.Recibo
import com.example.gestionalquileres.domain.model.onFailure
import com.example.gestionalquileres.domain.model.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ReciboViewModel @Inject constructor(
    private val reciboRepository: ReciboRepositoryImpl,
    private val contratoRepository: ContratoRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReciboUiState())
    val uiState: StateFlow<ReciboUiState> = _uiState.asStateFlow()

    // ---------- Helpers privados ----------

    private fun iniciarOperacion() =
        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

    private fun fallo(error: Exception, mensajePorDefecto: String) =
        _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: mensajePorDefecto) }

    private fun mostrarError(mensaje: String) =
        _uiState.update { it.copy(errorMessage = mensaje) }

    private fun String.aMontoDouble(): Double =
        trim().replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0)?.redondearDosDecimales() ?: 0.0

    private fun Double.redondearDosDecimales(): Double =
        BigDecimal.valueOf(this).setScale(2, RoundingMode.HALF_UP).toDouble()

    // ---------- Navegación interna ----------

    fun mostrarFormularioRegistro(mostrar: Boolean) =
        _uiState.update { it.copy(mostrandoFormularioRegistro = mostrar) }

    fun volverAlListado() =
        _uiState.update { it.copy(mostrandoFormularioRegistro = false) }

    fun limpiarMensajes() =
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }

    // ---------- Búsqueda y filtros rápidos ----------

    fun actualizarTextoBusqueda(texto: String) =
        _uiState.update { it.copy(textoBusqueda = texto) }

    fun actualizarFiltroEstado(filtro: String) {
        val filtrosPermitidos = setOf(
            ReciboUiState.FILTRO_TODOS,
            ReciboUiState.FILTRO_PENDIENTES,
            ReciboUiState.FILTRO_COBRADOS
        )
        if (filtro in filtrosPermitidos) {
            _uiState.update { it.copy(filtroEstado = filtro) }
        }
    }

    // ---------- Contratos (formulario y filtro) ----------

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

    fun cargarContratosParaCobro() = cargarContratos()

    // ---------- Generar recibo ----------

    fun generarRecibo(
        contrato: Contrato,
        inputAgua: String,
        inputLuz: String,
        inputMantenimiento: String = "",
        inputIpc: String = ""
    ) {
        viewModelScope.launch {
            iniciarOperacion()

            val agua = inputAgua.aMontoDouble()
            val luz = inputLuz.aMontoDouble()
            val mantenimiento = inputMantenimiento.aMontoDouble()
            val ipc = inputIpc.aMontoDouble()
            val renta = contrato.precioRentaAcordado.redondearDosDecimales()

            val igv = BigDecimal.valueOf(renta)
                .multiply(BigDecimal("0.18"))
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()

            val total = BigDecimal.valueOf(renta)
                .add(BigDecimal.valueOf(igv))
                .add(BigDecimal.valueOf(agua))
                .add(BigDecimal.valueOf(luz))
                .add(BigDecimal.valueOf(mantenimiento))
                .add(BigDecimal.valueOf(ipc))
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()

            val reciboParcial = Recibo(
                idContrato = contrato.idContrato,
                nombreInquilino = contrato.nombreInquilino,
                dniInquilino = contrato.dniInquilino,
                direccionInmueble = contrato.direccionInmueble,
                distritoInmueble = contrato.distritoInmueble,
                nombreUnidad = contrato.nombreUnidad,
                fechaEmision = LocalDate.now().toString(),
                rentaBase = renta,
                montoIgv = igv,
                agua = agua,
                luz = luz,
                mantenimiento = mantenimiento,
                montoTotal = total,
                estadoPago = EstadoPago.PENDIENTE.valor
            )

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

    fun cargarHistorialRecibos() {
        val filtro = _uiState.value.filtro
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            reciboRepository.obtenerPagina(filtro)
                .onSuccess { pagina ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            recibos = pagina.items.distinctBy { item -> item.idRecibo },
                            siguientePagina = pagina.siguiente
                        )
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
                        it.copy(
                            cargandoMas = false,
                            errorMessage = error.message ?: "No se pudieron cargar más recibos."
                        )
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
        _uiState.update { it.copy(textoBusqueda = "", filtroEstado = ReciboUiState.FILTRO_TODOS) }
        aplicarFiltro(FiltroRecibos())
    }

    // ---------- Control de cobros ----------

    fun cambiarEstadoPago(recibo: Recibo) {
        if (_uiState.value.idReciboActualizando != null) return
        val estaCobrado = recibo.estado() == EstadoPago.PAGADO
        val nuevoEstadoEnum = if (estaCobrado) EstadoPago.PENDIENTE else EstadoPago.PAGADO
        val nuevoEstadoTexto = nuevoEstadoEnum.valor

        _uiState.update { it.copy(idReciboActualizando = recibo.idRecibo, errorMessage = null) }

        viewModelScope.launch {
            reciboRepository.actualizarEstadoPago(recibo.idRecibo, nuevoEstadoTexto)
                .onSuccess {
                    _uiState.update { estado ->
                        val filtroEstado = estado.filtro.estado
                        val actualizados = estado.recibos.mapNotNull { actual ->
                            when {
                                actual.idRecibo != recibo.idRecibo -> actual
                                filtroEstado != null && filtroEstado != nuevoEstadoEnum -> null
                                else -> actual.copy(estadoPago = nuevoEstadoTexto)
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