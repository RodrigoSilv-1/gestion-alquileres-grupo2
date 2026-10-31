package com.example.gestionalquileres.ui.contrato

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionalquileres.data.repository.ContratoRepository
import com.example.gestionalquileres.data.repository.InmuebleRepository
import com.example.gestionalquileres.data.repository.InquilinoRepository
import com.example.gestionalquileres.data.repository.UnidadAlquilableRepository
import com.example.gestionalquileres.domain.comoMonto
import com.example.gestionalquileres.domain.cumpleSolvencia
import com.example.gestionalquileres.domain.ingresoMinimoRequerido
import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Contrato
import com.example.gestionalquileres.domain.model.Inmueble
import com.example.gestionalquileres.domain.model.Inquilino
import com.example.gestionalquileres.domain.model.UnidadAlquilable
import com.example.gestionalquileres.domain.model.onFailure
import com.example.gestionalquileres.domain.model.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ContratoViewModel @Inject constructor(
    private val contratoRepository: ContratoRepository,
    private val inquilinoRepository: InquilinoRepository,
    private val inmuebleRepository: InmuebleRepository,
    private val unidadAlquilableRepository: UnidadAlquilableRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContratoUiState())
    val uiState: StateFlow<ContratoUiState> = _uiState.asStateFlow()

    // ------------------------------------------------------------------
    // Helpers privados
    // ------------------------------------------------------------------

    private fun iniciarOperacion() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
    }

    private fun fallo(mensaje: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = mensaje) }
    }

    /** Error de validación: no hubo operación en curso, así que no toca isLoading. */
    private fun mostrarError(mensaje: String) {
        _uiState.update { it.copy(errorMessage = mensaje) }
    }

    // ------------------------------------------------------------------
    // Carga de datos
    // ------------------------------------------------------------------

    fun cargarDatosIniciales() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Las tres lecturas independientes se lanzan en paralelo
            val (contratosResult, inquilinosResult, inmueblesResult) = coroutineScope {
                val contratos = async { contratoRepository.obtenerContratosActivos() }
                val inquilinos = async { inquilinoRepository.obtenerTodos() }
                val inmuebles = async { inmuebleRepository.obtenerTodos() }
                Triple(contratos.await(), inquilinos.await(), inmuebles.await())
            }

            if (contratosResult is AppResult.Exito &&
                inquilinosResult is AppResult.Exito &&
                inmueblesResult is AppResult.Exito
            ) {
                val unidadesDisponibles = cargarUnidadesDisponibles(inmueblesResult.valor)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        contratosActivos = contratosResult.valor,
                        listaInquilinos = inquilinosResult.valor,
                        listaUnidadesDisponibles = unidadesDisponibles
                    )
                }
            } else {
                fallo("Error al cargar los datos para la gestión de contratos.")
            }
        }
    }

    /** Pide las unidades de todos los inmuebles a la vez en lugar de una consulta tras otra. */
    private suspend fun cargarUnidadesDisponibles(
        inmuebles: List<Inmueble>
    ): List<Pair<Inmueble, UnidadAlquilable>> = coroutineScope {
        inmuebles
            .map { inmueble ->
                async { inmueble to unidadAlquilableRepository.obtenerUnidadesPorInmueble(inmueble.idInmueble) }
            }
            .awaitAll()
            .flatMap { (inmueble, resultado) ->
                if (resultado is AppResult.Exito) {
                    resultado.valor
                        .filter { !it.estaOcupada && it.precioRenta > 0.0 }
                        .map { unidad -> inmueble to unidad }
                } else {
                    emptyList()
                }
            }
    }

    // ------------------------------------------------------------------
    // Formulario
    // ------------------------------------------------------------------

    /** Deja el formulario en blanco. Se llama al abrirlo para que no queden datos de un intento anterior. */
    fun prepararNuevoContrato() {
        _uiState.update {
            it.copy(
                inquilinoSeleccionado = null,
                inmuebleSeleccionado = null,
                unidadSeleccionada = null,
                fechaInicio = "",
                fechaFin = "",
                esSolvente = null,
                mensajeSolvencia = "",
                errorMessage = null
            )
        }
    }

    fun seleccionarInquilino(inquilino: Inquilino) {
        _uiState.update { it.copy(inquilinoSeleccionado = inquilino).conSolvenciaEvaluada() }
    }

    fun seleccionarUnidad(inmueble: Inmueble, unidad: UnidadAlquilable) {
        _uiState.update {
            it.copy(inmuebleSeleccionado = inmueble, unidadSeleccionada = unidad).conSolvenciaEvaluada()
        }
    }

    fun actualizarFechas(inicio: String, fin: String) {
        _uiState.update { it.copy(fechaInicio = inicio, fechaFin = fin) }
    }

    /** La regla vive en domain/Solvencia.kt; aquí solo se arma el estado y el mensaje. */
    private fun ContratoUiState.conSolvenciaEvaluada(): ContratoUiState {
        val inquilino = inquilinoSeleccionado
        val unidad = unidadSeleccionada
        if (inquilino == null || unidad == null) {
            return copy(esSolvente = null, mensajeSolvencia = "")
        }

        val renta = unidad.precioRenta
        if (renta <= 0.0) {
            return copy(
                esSolvente = false,
                mensajeSolvencia = "La unidad seleccionada no tiene un precio de renta válido (${renta.comoMonto()})."
            )
        }

        val sueldo = inquilino.sueldo
        val minimo = ingresoMinimoRequerido(renta)
        val esApto = cumpleSolvencia(sueldo, renta)
        val mensaje = if (esApto) {
            "Solvencia Aprobada: El sueldo (${sueldo.comoMonto()}) cumple con el requisito mínimo del doble de la renta (${minimo.comoMonto()})."
        } else {
            "Solvencia Rechazada: El sueldo (${sueldo.comoMonto()}) no alcanza el doble de la renta requerida (${minimo.comoMonto()})."
        }
        return copy(esSolvente = esApto, mensajeSolvencia = mensaje)
    }

    // ------------------------------------------------------------------
    // Registro y finalización
    // ------------------------------------------------------------------

    fun registrarContrato() {
        val actual = _uiState.value
        val inquilino = actual.inquilinoSeleccionado
        val inmueble = actual.inmuebleSeleccionado
        val unidad = actual.unidadSeleccionada

        if (inquilino == null || inmueble == null || unidad == null) {
            mostrarError("Debe seleccionar un inquilino y una unidad.")
            return
        }
        if (actual.esSolvente != true) {
            mostrarError("No se puede registrar el contrato: El inquilino no cuenta con solvencia aprobada.")
            return
        }
        if (actual.fechaInicio.isBlank() || actual.fechaFin.isBlank()) {
            mostrarError("Las fechas de inicio y fin son obligatorias.")
            return
        }

        val inicio = runCatching { LocalDate.parse(actual.fechaInicio) }.getOrNull()
        val fin = runCatching { LocalDate.parse(actual.fechaFin) }.getOrNull()

        if (inicio == null || fin == null) {
            mostrarError("Formato de fecha inválido.")
            return
        }
        if (inicio.isBefore(LocalDate.now())) {
            mostrarError("La fecha de inicio no puede ser anterior a la fecha actual.")
            return
        }
        if (!fin.isAfter(inicio)) {
            mostrarError("La fecha de fin debe ser posterior a la fecha de inicio.")
            return
        }
        if (fin.isBefore(inicio.plusMonths(DURACION_MINIMA_MESES))) {
            mostrarError("El contrato debe tener una duración mínima de $DURACION_MINIMA_MESES meses.")
            return
        }

        viewModelScope.launch {
            iniciarOperacion()

            // Respaldo: otro usuario pudo crear un contrato para este inquilino mientras el formulario estaba abierto
            val existente = contratoRepository.obtenerContratoActivoPorInquilino(inquilino.idInquilino)
            if (existente is AppResult.Exito && existente.valor != null) {
                fallo("Este inquilino ya cuenta con un contrato activo vigente en otra unidad.")
                return@launch
            }

            val nuevoContrato = Contrato(
                inmuebleId = inmueble.idInmueble,
                direccionInmueble = inmueble.direccion,
                distritoInmueble = inmueble.distrito,
                unidadId = unidad.id,
                nombreUnidad = unidad.nombreFormateado,
                inquilinoId = inquilino.idInquilino,
                nombreInquilino = inquilino.nombre,
                dniInquilino = inquilino.dni,
                precioRentaAcordado = unidad.precioRenta,
                ingresoMensualInquilino = inquilino.sueldo,
                fechaInicio = actual.fechaInicio,
                fechaFin = actual.fechaFin
            )

            contratoRepository.registrarContrato(nuevoContrato)
                .onSuccess {
                    _uiState.update { estado ->
                        estado.copy(
                            isLoading = false,
                            successMessage = "Contrato registrado con éxito.",
                            inquilinoSeleccionado = null,
                            inmuebleSeleccionado = null,
                            unidadSeleccionada = null,
                            fechaInicio = "",
                            fechaFin = "",
                            esSolvente = null,
                            mensajeSolvencia = ""
                        )
                    }
                    cargarDatosIniciales()
                }
                .onFailure { error -> fallo(error.message ?: "Error al registrar el contrato.") }
        }
    }

    fun finalizarContrato(idContrato: String, inmuebleId: String, unidadId: String) {
        viewModelScope.launch {
            iniciarOperacion()
            contratoRepository.finalizarContrato(idContrato, inmuebleId, unidadId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Contrato finalizado y unidad liberada correctamente."
                        )
                    }
                    cargarDatosIniciales()
                }
                .onFailure { error -> fallo(error.message ?: "Error al finalizar el contrato.") }
        }
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}