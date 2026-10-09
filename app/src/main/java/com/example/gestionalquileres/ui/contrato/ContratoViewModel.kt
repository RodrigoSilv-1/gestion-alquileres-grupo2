package com.example.gestionalquileres.ui.contrato

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionalquileres.data.repository.ContratoRepository
import com.example.gestionalquileres.data.auth.repository.InmuebleRepository
import com.example.gestionalquileres.data.auth.repository.InquilinoRepository
import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Contrato
import com.example.gestionalquileres.domain.model.Inquilino
import com.example.gestionalquileres.domain.model.Inmueble
import com.example.gestionalquileres.domain.model.UnidadAlquilable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ContratoViewModel @Inject constructor(
    private val contratoRepository: ContratoRepository,
    private val inquilinoRepository: InquilinoRepository,
    private val inmuebleRepository: InmuebleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContratoUiState())
    val uiState: StateFlow<ContratoUiState> = _uiState.asStateFlow()

    fun cargarDatosIniciales() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val contratosResult = contratoRepository.obtenerContratosActivos()
            val inquilinosResult = inquilinoRepository.obtenerTodos()
            val inmueblesResult = inmuebleRepository.obtenerTodos()

            if (contratosResult is AppResult.Exito && inquilinosResult is AppResult.Exito && inmueblesResult is AppResult.Exito) {
                // NOTA: Si tu clase AppResult usa una variable distinta a "data" (como "datos" o "valor"), cámbiala aquí
                val inmuebles = inmueblesResult.valor
                val unidadesDisponiblesList = mutableListOf<Pair<Inmueble, UnidadAlquilable>>()
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()

                for (inmueble in inmuebles) {
                    try {
                        val snapshot = firestore.collection("inmuebles")
                            .document(inmueble.idInmueble)
                            .collection("unidadesAlquilables")
                            .get()
                            .await()

                        val unidades = snapshot.documents.mapNotNull { it.toObject(UnidadAlquilable::class.java) }
                        for (unidad in unidades) {
                            if (!unidad.estaOcupada && unidad.precioRenta > 0.0) {
                                unidadesDisponiblesList.add(Pair(inmueble, unidad))
                            }
                        }
                    } catch (e: Exception) { }
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    contratosActivos = contratosResult.valor,
                    listaInquilinos = inquilinosResult.valor,
                    listaUnidadesDisponibles = unidadesDisponiblesList
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error al cargar los datos para la gestión de contratos."
                )
            }
        }
    }

    fun seleccionarInquilino(inquilino: Inquilino) {
        _uiState.value = _uiState.value.copy(inquilinoSeleccionado = inquilino)
        evaluarSolvencia()
    }

    fun seleccionarUnidad(inmueble: Inmueble, unidad: UnidadAlquilable) {
        _uiState.value = _uiState.value.copy(
            inmuebleSeleccionado = inmueble,
            unidadSeleccionada = unidad
        )
        evaluarSolvencia()
    }

    fun actualizarFechas(inicio: String, fin: String) {
        _uiState.value = _uiState.value.copy(fechaInicio = inicio, fechaFin = fin)
    }

    private fun evaluarSolvencia() {
        val inquilino = _uiState.value.inquilinoSeleccionado
        val unidad = _uiState.value.unidadSeleccionada

        if (inquilino == null || unidad == null) {
            _uiState.value = _uiState.value.copy(esSolvente = null, mensajeSolvencia = "")
            return
        }

        val sueldo = inquilino.sueldo
        val renta = unidad.precioRenta

        if (renta <= 0.0) {
            _uiState.value = _uiState.value.copy(
                esSolvente = false,
                mensajeSolvencia = "La unidad seleccionada no tiene un precio de renta válido ($0.0)."
            )
            return
        }

        val rentaMinimaRequerida = renta * 2.0
        val esApto = sueldo >= rentaMinimaRequerida

        val mensaje = if (esApto) {
            "Solvencia Aprobada: El sueldo ($$sueldo) cumple con el requisito mínimo del doble de la renta ($$rentaMinimaRequerida)."
        } else {
            "Solvencia Rechazada: El sueldo ($$sueldo) no alcanza el doble de la renta requerida ($$rentaMinimaRequerida)."
        }

        _uiState.value = _uiState.value.copy(
            esSolvente = esApto,
            mensajeSolvencia = mensaje
        )
    }

    fun registrarContrato() {
        val estado = _uiState.value
        val inquilino = estado.inquilinoSeleccionado
        val inmueble = estado.inmuebleSeleccionado
        val unidad = estado.unidadSeleccionada

        if (inquilino == null || inmueble == null || unidad == null) {
            _uiState.value = estado.copy(errorMessage = "Debe seleccionar un inquilino y una unidad.")
            return
        }

        if (estado.esSolvente != true) {
            _uiState.value = estado.copy(errorMessage = "No se puede registrar el contrato: El inquilino no cuenta con solvencia aprobada.")
            return
        }

        if (estado.fechaInicio.isBlank() || estado.fechaFin.isBlank()) {
            _uiState.value = estado.copy(errorMessage = "Las fechas de inicio y fin son obligatorias.")
            return
        }

        try {
            val partesInicio = estado.fechaInicio.split("-")
            val partesFin = estado.fechaFin.split("-")

            if (partesInicio.size == 3 && partesFin.size == 3) {
                val anioInicio = partesInicio[0].toInt()
                val mesInicio = partesInicio[1].toInt()
                val diaInicio = partesInicio[2].toInt()

                val anioFin = partesFin[0].toInt()
                val mesFin = partesFin[1].toInt()
                val diaFin = partesFin[2].toInt()

                val fechaActualCalendar = java.util.Calendar.getInstance()
                val calInicio = java.util.Calendar.getInstance().apply {
                    set(anioInicio, mesInicio - 1, diaInicio)
                }

                fechaActualCalendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                fechaActualCalendar.set(java.util.Calendar.MINUTE, 0)
                fechaActualCalendar.set(java.util.Calendar.SECOND, 0)
                fechaActualCalendar.set(java.util.Calendar.MILLISECOND, 0)

                if (calInicio.before(fechaActualCalendar)) {
                    _uiState.value = estado.copy(errorMessage = "La fecha de inicio no puede ser anterior a la fecha actual.")
                    return
                }

                val totalMesesInicio = anioInicio * 12 + mesInicio
                val totalMesesFin = anioFin * 12 + mesFin
                val diferenciaMeses = totalMesesFin - totalMesesInicio

                if (diferenciaMeses < 0 || (diferenciaMeses == 0 && diaFin <= diaInicio)) {
                    _uiState.value = estado.copy(errorMessage = "La fecha de fin debe ser posterior a la fecha de inicio.")
                    return
                }

                if (diferenciaMeses < 3 || (diferenciaMeses == 3 && diaFin < diaInicio)) {
                    _uiState.value = estado.copy(errorMessage = "El contrato debe tener una duración mínima de 3 meses.")
                    return
                }
            } else {
                _uiState.value = estado.copy(errorMessage = "Formato de fecha inválido. Usa YYYY-MM-DD.")
                return
            }
        } catch (e: Exception) {
            _uiState.value = estado.copy(errorMessage = "Error al validar las fechas.")
            return
        }

        viewModelScope.launch {
            _uiState.value = estado.copy(isLoading = true, errorMessage = null)

            val contratoExistenteResult = contratoRepository.obtenerContratoActivoPorInquilino(inquilino.idInquilino)

            if (contratoExistenteResult is AppResult.Exito && contratoExistenteResult.valor != null) {
                _uiState.value = estado.copy(
                    isLoading = false,
                    errorMessage = "Este inquilino ya cuenta con un contrato activo vigente en otra unidad."
                )
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
                fechaInicio = estado.fechaInicio,
                fechaFin = estado.fechaFin
            )

            when (val resultado = contratoRepository.registrarContrato(nuevoContrato)) {
                is AppResult.Exito -> {
                    _uiState.value = estado.copy(
                        isLoading = false,
                        successMessage = "Contrato registrado con éxito.",
                        inquilinoSeleccionado = null,
                        inmuebleSeleccionado = null,
                        unidadSeleccionada = null,
                        esSolvente = null,
                        mensajeSolvencia = ""
                    )
                    cargarDatosIniciales()
                }
                is AppResult.Error -> {
                    _uiState.value = estado.copy(
                        isLoading = false,
                        errorMessage = resultado.excepcion.message ?: "Error al registrar el contrato."
                    )
                }
            }
        }
    }

    fun finalizarContrato(idContrato: String, inmuebleId: String, unidadId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            when (val resultado = contratoRepository.finalizarContrato(idContrato, inmuebleId, unidadId)) {
                is AppResult.Exito -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Contrato finalizado y unidad liberada correctamente."
                    )
                    cargarDatosIniciales()
                }
                is AppResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = resultado.excepcion.message ?: "Error al finalizar el contrato."
                    )
                }
            }
        }
    }

    fun limpiarMensajes() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}