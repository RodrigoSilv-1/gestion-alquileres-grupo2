package com.example.gestionalquileres.ui.inquilinos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionalquileres.data.repository.InquilinoRepository
import com.example.gestionalquileres.domain.model.Inquilino
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InquilinoViewModel : ViewModel() {

    private val repository = InquilinoRepository()

    // Estado principal expuesto a la interfaz
    private val _uiState = MutableStateFlow(InquilinoUiState())
    val uiState: StateFlow<InquilinoUiState> = _uiState.asStateFlow()

    // Registra validando primero si el DNI ya existia en el sistema
    fun registrar(inquilino: Inquilino) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            // Revisa si el DNI ya esta en la base de datos
            repository.buscarPorDniCualquiera(inquilino.dni)
                .onSuccess { inquilinoExistente ->
                    if (inquilinoExistente != null) {
                        _uiState.value = _uiState.value.copy(isLoading = false)

                        if (!inquilinoExistente.active) {
                            // Si estaba de baja, prepara el objeto para ofrecer reactivacion
                            val inquilinoParaReactivar = inquilino.copy(
                                idInquilino = inquilinoExistente.idInquilino
                            )
                            _uiState.value = _uiState.value.copy(
                                inquilinoInactivoDetectado = inquilinoParaReactivar
                            )
                        } else {
                            // Si ya existe activo, avisa para evitar duplicados
                            _uiState.value = _uiState.value.copy(
                                errorMessage = "Ya existe un inquilino activo con el DNI ${inquilino.dni}."
                            )
                        }
                    } else {
                        // Si no existe, procede con el alta normal
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

    // Consulta en Firestore todos los inquilinos activos
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

    // Busca inquilinos activos por numero de DNI
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

    // Busca inquilinos activos por coincidencia de nombre
    fun obtenerPorNombre(nombre: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            repository.obtenerPorNombre(nombre)
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
                            ?: "No se pudo buscar por nombre."
                    )
                }
        }
    }

    // Actualiza los datos de un inquilino existente y recarga la lista
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
                        successMessage = "Datos del inquilino actualizados correctamente."
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

    // Aplica baja logica y actualiza la lista en pantalla
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
                        successMessage = "Inquilino dado de baja correctamente."
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

    // Inserta un registro nuevo y lo anexa a la lista local
    private suspend fun ejecutarRegistroNuevo(inquilino: Inquilino) {
        repository.registrar(inquilino)
            .onSuccess { nuevoInquilino ->
                val listaActualizada = _uiState.value.inquilinos + nuevoInquilino
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    inquilinos = listaActualizada,
                    successMessage = "Inquilino registrado correctamente."
                )
            }
            .onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "No se pudo registrar el inquilino."
                )
            }
    }

    // Cambia active a true y suma el inquilino de vuelta a la lista activa
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
                        successMessage = "Inquilino reactivado con éxito."
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

    // Cierra el dialogo de reactivacion si el usuario cancela
    fun cancelarReactivacion() {
        _uiState.value = _uiState.value.copy(
            inquilinoInactivoDetectado = null,
            errorMessage = null
        )
    }

    // Limpia alertas de error o confirmacion
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