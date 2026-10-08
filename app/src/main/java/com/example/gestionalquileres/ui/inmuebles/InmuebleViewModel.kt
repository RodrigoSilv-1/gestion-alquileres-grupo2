package com.example.gestionalquileres.ui.inmuebles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionalquileres.domain.model.onSuccess
import com.example.gestionalquileres.domain.model.onFailure
import com.example.gestionalquileres.data.repository.InmuebleRepository
import com.example.gestionalquileres.data.auth.repository.InmuebleRepository
import com.example.gestionalquileres.data.repository.InquilinoRepository
import com.example.gestionalquileres.domain.model.Inmueble
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.gestionalquileres.domain.model.Inquilino
import com.example.gestionalquileres.ui.inquilinos.InquilinoUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InmuebleViewModel @Inject constructor(
    private val repository: InmuebleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InmuebleUiState())
    val uiState: StateFlow<InmuebleUiState> = _uiState.asStateFlow()

    // Registro del inmueble
    fun registrar(inmueble: Inmueble) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            ejecutarRegistroNuevo(inmueble)
        }
    }

    // --- FUNCIONES DE LECTURA ---

    fun obtenerTodos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            repository.obtenerTodos()
                .onSuccess { lista ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        inmuebles = lista
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                            ?: "No se pudieron obtener los inmuebles."
                    )
                }
        }
    }

    fun obtenerPorCodigoPostal(codigoPostal: String) {
        // Asume que tienes esta función en tu repositorio
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            repository.obtenerPorCodigoPostal(codigoPostal)
                .onSuccess { lista ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        inmuebles = lista
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                            ?: "No se pudo buscar por código postal."
                    )
                }
        }
    }

    // Busca inmuebles activos por coincidencia de distrito
    fun obtenerPorDistrito(distrito: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            repository.obtenerporDistrito(distrito)
                .onSuccess { lista ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        inmuebles = lista
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                            ?: "No se pudo buscar por distrito."
                    )
                }
        }
    }

    fun actualizar(inmueble: Inmueble) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            repository.actualizar(inmueble)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Datos del inmueble actualizados correctamente."
                    )

                    obtenerTodos()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                            ?: "No se pudo actualizar el inmueble."
                    )
                }
        }
    }

    fun darDeBaja(idInmueble: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            repository.darDeBaja(idInmueble)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Inmueble dado de baja correctamente."
                    )

                    obtenerTodos()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                            ?: "No se pudo dar de baja al inmueble."
                    )
                }
        }
    }

    // Inserta un registro nuevo y lo anexa a la lista local
    private suspend fun ejecutarRegistroNuevo(inmueble: Inmueble) {
        repository.registrar(inmueble)
            .onSuccess { nuevoInmueble ->
                val listaActualizada = _uiState.value.inmuebles + nuevoInmueble
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    inmuebles = listaActualizada,
                    successMessage = "Inmueble registrado correctamente."
                )
            }
            .onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "No se pudo registrar el inmueble."
                )
            }
    }

    // Cambia active a true y suma el inmueble de vuelta a la lista activa
    fun reactivarInmueble(inmueble: Inmueble) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null,
                inmuebleInactivoDetectado = null
            )

            repository.reactivarYActualizar(inmueble)
                .onSuccess { inmuebleReactivado ->
                    val listaActualizada = _uiState.value.inmuebles + inmuebleReactivado
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        inmuebles = listaActualizada,
                        successMessage = "Inmueble reactivado con éxito."
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "No se pudo reactivar el inmueble."
                    )
                }
        }
    }

    // Actualiza el texto del número de pisos en el estado
    fun onNumeroPisosChanged(numero: String) {
        _uiState.value = _uiState.value.copy(numeroPisos = numero)
    }

    // Actualiza el texto de las unidades por piso en el estado
    fun onUnidadesPorPisoChanged(unidades: String) {
        _uiState.value = _uiState.value.copy(unidadesPorPiso = unidades)
    }

    // Consulta las unidades alquilables de un inmueble específico usando su subcolección
    fun obtenerUnidadesPorInmueble(inmuebleId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val unidadRepository = com.example.gestionalquileres.data.auth.repository.UnidadAlquilableRepository()

            unidadRepository.obtenerUnidadesPorInmueble(inmuebleId)
                .onSuccess { listaUnidades ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        unidadesAlquilables = listaUnidades
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "No se pudieron obtener las unidades alquilables."
                    )
                }
        }
    }

    fun actualizarPrecioUnidad(inmuebleId: String, unidadId: String, nuevoPrecio: Double) {
        viewModelScope.launch {
            val unidadRepository = com.example.gestionalquileres.data.auth.repository.UnidadAlquilableRepository()
            unidadRepository.actualizarPrecioRenta(inmuebleId, unidadId, nuevoPrecio)
                .onSuccess {
                    obtenerUnidadesPorInmueble(inmuebleId) // Refresca la lista
                }
        }
    }

    // Cierra el dialogo de reactivación si el usuario cancela
    fun cancelarReactivacion() {
        _uiState.value = _uiState.value.copy(
            inmuebleInactivoDetectado = null,
            errorMessage = null
        )
    }

    // Limpia alertas de error o confirmación
    fun limpiarMensajes() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    // Quita la selección del inmueble actual
    fun limpiarInmuebleSeleccionado() {
        _uiState.value = _uiState.value.copy(
            inmuebleSeleccionado = null
        )
    }
}