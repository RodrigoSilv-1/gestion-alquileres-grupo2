package com.example.gestionalquileres.ui.inquilinos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionalquileres.data.auth.repository.InquilinoRepository
import com.example.gestionalquileres.domain.model.Inquilino
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.gestionalquileres.domain.model.onSuccess
import com.example.gestionalquileres.domain.model.onFailure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel // CAMBIO 2: Le decimos a Hilt que administre este ViewModel
class InquilinoViewModel @Inject constructor(
    private val repository: InquilinoRepository // CAMBIO 3: Hilt inyecta el repositorio automáticamente
) : ViewModel() {


    private val _uiState = MutableStateFlow(InquilinoUiState())
    val uiState: StateFlow<InquilinoUiState> = _uiState.asStateFlow()

    // --- NUEVAS FUNCIONES DE ESTADO DESACOPLADO ---

    fun actualizarTextoBusqueda(texto: String) {
        _uiState.value = _uiState.value.copy(textoBusqueda = texto)
    }

    fun actualizarTipoBusqueda(tipo: String) {
        _uiState.value = _uiState.value.copy(tipoBusqueda = tipo)
    }

    fun mostrarFormularioRegistro(mostrar: Boolean) {
        _uiState.value = _uiState.value.copy(mostrandoFormularioRegistro = mostrar)
    }

    fun seleccionarInquilinoParaEditar(inquilino: Inquilino?) {
        _uiState.value = _uiState.value.copy(inquilinoSeleccionado = inquilino)
    }

    fun seleccionarInquilinoParaBaja(inquilino: Inquilino?) {
        _uiState.value = _uiState.value.copy(inquilinoDarDeBaja = inquilino)
    }

    // --- FUNCIONES DE BASE DE DATOS INTACTAS ---

    fun registrar(inquilino: Inquilino) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            repository.buscarPorDniCualquiera(inquilino.dni)
                .onSuccess { inquilinoExistente ->
                    if (inquilinoExistente != null) {
                        _uiState.value = _uiState.value.copy(isLoading = false)

                        if (!inquilinoExistente.active) {
                            val inquilinoParaReactivar = inquilino.copy(
                                idInquilino = inquilinoExistente.idInquilino
                            )
                            _uiState.value = _uiState.value.copy(
                                inquilinoInactivoDetectado = inquilinoParaReactivar
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                errorMessage = "Ya existe un inquilino activo con el DNI ${inquilino.dni}."
                            )
                        }
                    } else {
                        ejecutarRegistroNuevo(inquilino)
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Error al validar el DNI."
                    )
                }
        }
    }

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
                        inquilinos = lista
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                            ?: "No se pudieron obtener los inquilinos."
                    )
                }
        }
    }

    fun obtenerPorDni(dni: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            repository.obtenerPorDni(dni)
                .onSuccess { lista ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        inquilinos = lista
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                            ?: "No se pudo buscar por DNI."
                    )
                }
        }
    }

    fun obtenerPorNombre(nombre: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            repository.obtenerPorNombre(nombre)
                .onSuccess { lista ->
                    _uiState.value = _uiState.value.copy(isLoading = false, inquilinos = lista)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "No se pudo buscar por nombre."
                    )
                }
        }
    }

    fun actualizar(inquilino: Inquilino) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            repository.actualizar(inquilino)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Datos del inquilino actualizados correctamente.",
                        inquilinoSeleccionado = null // Cierra la ventana de edición al terminar
                    )
                    obtenerTodos()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                            ?: "No se pudo actualizar el inquilino."
                    )
                }
        }
    }

    fun darDeBaja(idInquilino: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            repository.darDeBaja(idInquilino)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Inquilino dado de baja correctamente.",
                        inquilinoDarDeBaja = null // Cierra la ventana de confirmación al terminar
                    )
                    obtenerTodos()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                            ?: "No se pudo dar de baja al inquilino."
                    )
                }
        }
    }

    private suspend fun ejecutarRegistroNuevo(inquilino: Inquilino) {
        repository.registrar(inquilino)
            .onSuccess { nuevoInquilino ->
                val listaActualizada = _uiState.value.inquilinos + nuevoInquilino
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    inquilinos = listaActualizada,
                    successMessage = "Inquilino registrado correctamente.",
                    mostrandoFormularioRegistro = false // Cierra el formulario automáticamente
                )
            }
            .onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "No se pudo registrar el inquilino."
                )
            }
    }

    fun reactivarInquilino(inquilino: Inquilino) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null,
                inquilinoInactivoDetectado = null
            )

            repository.reactivarYActualizar(inquilino)
                .onSuccess { inquilinoReactivado ->
                    val listaActualizada = _uiState.value.inquilinos + inquilinoReactivado
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        inquilinos = listaActualizada,
                        successMessage = "Inquilino reactivado con éxito.",
                        mostrandoFormularioRegistro = false // Cierra el formulario
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "No se pudo reactivar el inquilino."
                    )
                }
        }
    }

    fun cancelarReactivacion() {
        _uiState.value = _uiState.value.copy(
            inquilinoInactivoDetectado = null,
            errorMessage = null
        )
    }

    fun limpiarMensajes() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    // Quita la seleccion del inquilino actual
    fun limpiarInquilinoSeleccionado() {
        _uiState.value = _uiState.value.copy(
            inquilinoSeleccionado = null
        )
    }
}