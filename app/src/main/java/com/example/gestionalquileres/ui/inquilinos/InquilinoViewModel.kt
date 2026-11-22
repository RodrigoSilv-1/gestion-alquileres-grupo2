package com.example.gestionalquileres.ui.inquilinos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionalquileres.data.repository.InquilinoRepositoryImpl
import com.example.gestionalquileres.domain.model.Inquilino
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.gestionalquileres.domain.model.onSuccess
import com.example.gestionalquileres.domain.model.onFailure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel // CAMBIO 2: Le decimos a Hilt que administre este ViewModel
class InquilinoViewModel @Inject constructor(
    private val repository: InquilinoRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(InquilinoUiState())
    val uiState: StateFlow<InquilinoUiState> = _uiState.asStateFlow()

    fun actualizarTextoBusqueda(texto: String) {
        _uiState.update { it.copy(textoBusqueda = texto) }
    }

    fun actualizarTipoBusqueda(tipo: String) {
        _uiState.update { it.copy(tipoBusqueda = tipo) }
    }

    fun mostrarFormularioRegistro(mostrar: Boolean) {
        _uiState.update { it.copy(mostrandoFormularioRegistro = mostrar) }
    }

    fun seleccionarInquilinoParaEditar(inquilino: Inquilino?) {
        _uiState.update { it.copy(inquilinoSeleccionado = inquilino) }
    }

    fun seleccionarInquilinoParaBaja(inquilino: Inquilino?) {
        _uiState.update { it.copy(inquilinoDarDeBaja = inquilino) }
    }

    fun registrar(inquilino: Inquilino) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    successMessage = null,
                    errorMessage = null
                )
            }

            repository.buscarPorDniCualquiera(inquilino.dni)
                .onSuccess { inquilinoExistente ->
                    if (inquilinoExistente != null) {
                        _uiState.update { it.copy(isLoading = false) }

                        if (!inquilinoExistente.active) {
                            val inquilinoParaReactivar = inquilino.copy(
                                idInquilino = inquilinoExistente.idInquilino
                            )
                            _uiState.update {
                                it.copy(inquilinoInactivoDetectado = inquilinoParaReactivar)
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    errorMessage = "Ya existe un inquilino activo con el DNI ${inquilino.dni}."
                                )
                            }
                        }
                    } else {
                        ejecutarRegistroNuevo(inquilino)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Error al validar el DNI."
                        )
                    }
                }
        }
    }

    fun obtenerTodos() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            repository.obtenerTodos()
                .onSuccess { lista ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            inquilinos = lista.distinctBy { item -> item.idInquilino }
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "No se pudieron obtener los inquilinos."
                        )
                    }
                }
        }
    }

    fun obtenerPorDni(dni: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            repository.obtenerPorDni(dni)
                .onSuccess { lista ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            inquilinos = lista.distinctBy { item -> item.idInquilino }
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "No se pudo buscar por DNI."
                        )
                    }
                }
        }
    }

    fun obtenerPorNombre(nombre: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            repository.obtenerPorNombre(nombre)
                .onSuccess { lista ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            inquilinos = lista.distinctBy { item -> item.idInquilino }
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "No se pudo buscar por nombre."
                        )
                    }
                }
        }
    }

    fun actualizar(inquilino: Inquilino) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    successMessage = null
                )
            }

            repository.actualizar(inquilino)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Datos del inquilino actualizados correctamente.",
                            inquilinoSeleccionado = null
                        )
                    }
                    obtenerTodos()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "No se pudo actualizar el inquilino."
                        )
                    }
                }
        }
    }

    fun darDeBaja(idInquilino: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    successMessage = null
                )
            }

            repository.darDeBaja(idInquilino)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Inquilino dado de baja correctamente.",
                            inquilinoDarDeBaja = null
                        )
                    }
                    obtenerTodos()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "No se pudo dar de baja al inquilino."
                        )
                    }
                }
        }
    }

    private suspend fun ejecutarRegistroNuevo(inquilino: Inquilino) {
        repository.registrar(inquilino)
            .onSuccess { nuevoInquilino ->
                val listaActualizada = (_uiState.value.inquilinos + nuevoInquilino)
                    .distinctBy { it.idInquilino }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        inquilinos = listaActualizada,
                        successMessage = "Inquilino registrado correctamente.",
                        mostrandoFormularioRegistro = false
                    )
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "No se pudo registrar el inquilino."
                    )
                }
            }
    }

    fun reactivarInquilino(inquilino: Inquilino) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    successMessage = null,
                    inquilinoInactivoDetectado = null
                )
            }

            repository.reactivarYActualizar(inquilino)
                .onSuccess { inquilinoReactivado ->
                    val listaActualizada = (_uiState.value.inquilinos + inquilinoReactivado)
                        .distinctBy { it.idInquilino }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            inquilinos = listaActualizada,
                            successMessage = "Inquilino reactivado con éxito.",
                            mostrandoFormularioRegistro = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "No se pudo reactivar el inquilino."
                        )
                    }
                }
        }
    }

    fun cancelarReactivacion() {
        _uiState.update {
            it.copy(
                inquilinoInactivoDetectado = null,
                errorMessage = null
            )
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

    fun limpiarInquilinoSeleccionado() {
        _uiState.update {
            it.copy(inquilinoSeleccionado = null)
        }
    }
}