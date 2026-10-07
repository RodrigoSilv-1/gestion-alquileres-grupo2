package com.example.gestionalquileres.ui.inmuebles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionalquileres.domain.model.onSuccess
import com.example.gestionalquileres.domain.model.onFailure
import com.example.gestionalquileres.data.repository.InmuebleRepository
import com.example.gestionalquileres.domain.model.Inmueble
import dagger.hilt.android.lifecycle.HiltViewModel
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

    // --- FUNCIONES DE LECTURA ---

    fun obtenerTodos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.obtenerTodos().onSuccess { lista ->
                _uiState.update { it.copy(inmuebles = lista, isLoading = false) }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Error al cargar inmuebles") }
            }
        }
    }

    fun obtenerPorDistrito(distrito: String) {
        // Asume que tienes esta función en tu repositorio (similar a obtenerPorNombre de inquilinos)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // repository.obtenerPorDistrito(distrito).onSuccess { ... }
            // Si aún no la tienes en el repo, por ahora llamamos a todos:
            obtenerTodos()
        }
    }

    fun obtenerPorCodigoPostal(codigoPostal: String) {
        // Asume que tienes esta función en tu repositorio
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // repository.obtenerPorCodigoPostal(codigoPostal).onSuccess { ... }
            obtenerTodos()
        }
    }

    // --- FUNCIONES DE ESCRITURA ---

    fun registrar(inmueble: Inmueble) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Aquí podrías validar si ya existe uno igual antes de guardar
            repository.registrar(inmueble).onSuccess {
                _uiState.update { state ->
                    state.copy(
                        mostrandoFormularioRegistro = false,
                        successMessage = "Inmueble registrado correctamente"
                    )
                }
                obtenerTodos() // Refresca la lista
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Error al registrar") }
            }
        }
    }

    fun actualizar(inmueble: Inmueble) {
        viewModelScope.launch {
            repository.actualizar(inmueble).onSuccess {
                _uiState.update { state ->
                    state.copy(
                        inmuebleSeleccionado = null,
                        successMessage = "Inmueble actualizado"
                    )
                }
                obtenerTodos()
            }
        }
    }

    fun darDeBaja(idInmueble: String) {
        viewModelScope.launch {
            repository.darDeBaja(idInmueble).onSuccess {
                _uiState.update { state ->
                    state.copy(
                        inmuebleDarDeBaja = null,
                        successMessage = "Inmueble dado de baja"
                    )
                }
                obtenerTodos()
            }
        }
    }

    fun reactivarInmueble(inmueble: Inmueble) {
        viewModelScope.launch {
            repository.reactivarYActualizar(inmueble).onSuccess {
                _uiState.update { state ->
                    state.copy(
                        inmuebleInactivoDetectado = null,
                        mostrandoFormularioRegistro = false,
                        successMessage = "Inmueble reactivado"
                    )
                }
                obtenerTodos()
            }
        }
    }

    // --- CONTROLADORES DE INTERFAZ (Eventos) ---

    fun mostrarFormularioRegistro(mostrar: Boolean) {
        _uiState.update { it.copy(mostrandoFormularioRegistro = mostrar) }
    }

    fun seleccionarInmuebleParaEditar(inmueble: Inmueble?) {
        _uiState.update { it.copy(inmuebleSeleccionado = inmueble) }
    }

    fun seleccionarInmuebleParaBaja(inmueble: Inmueble?) {
        _uiState.update { it.copy(inmuebleDarDeBaja = inmueble) }
    }

    fun cancelarReactivacion() {
        _uiState.update { it.copy(inmuebleInactivoDetectado = null) }
    }

    fun actualizarTipoBusqueda(tipo: String) {
        _uiState.update { it.copy(tipoBusqueda = tipo, textoBusqueda = "") }
    }

    fun actualizarTextoBusqueda(texto: String) {
        _uiState.update { it.copy(textoBusqueda = texto) }
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
}