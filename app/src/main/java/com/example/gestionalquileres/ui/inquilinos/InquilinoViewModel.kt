package com.example.gestionalquileres.ui.inquilinos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionalquileres.domain.repository.InquilinoRepository
import com.example.gestionalquileres.domain.model.Inquilino
import com.example.gestionalquileres.domain.model.onFailure
import com.example.gestionalquileres.domain.model.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InquilinoViewModel @Inject constructor(
    private val repository: InquilinoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InquilinoUiState())
    val uiState: StateFlow<InquilinoUiState> = _uiState.asStateFlow()

    // ---------- Navegación interna ----------

    fun mostrarFormularioRegistro(mostrar: Boolean) =
        _uiState.update { it.copy(mostrandoFormularioRegistro = mostrar) }

    fun seleccionarInquilinoParaEditar(inquilino: Inquilino?) =
        _uiState.update { it.copy(inquilinoSeleccionado = inquilino) }

    fun seleccionarInquilinoParaBaja(inquilino: Inquilino?) =
        _uiState.update { it.copy(inquilinoDarDeBaja = inquilino) }

    fun volverAlListado() = _uiState.update {
        it.copy(mostrandoFormularioRegistro = false, inquilinoSeleccionado = null)
    }

    fun cancelarReactivacion() =
        _uiState.update { it.copy(inquilinoInactivoDetectado = null, errorMessage = null) }

    fun limpiarMensajes() =
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }

    // ---------- Buscador (filtra en memoria, ver InquilinoUiState.inquilinosVisibles) ----------

    fun actualizarTextoBusqueda(texto: String) =
        _uiState.update { it.copy(textoBusqueda = texto) }

    fun actualizarTipoBusqueda(tipo: TipoBusquedaInquilino) =
        _uiState.update { it.copy(tipoBusqueda = tipo) }

    fun buscar() = _uiState.update {
        it.copy(filtroAplicado = it.textoBusqueda.trim(), tipoFiltroAplicado = it.tipoBusqueda)
    }

    fun mostrarTodos() {
        _uiState.update { it.copy(textoBusqueda = "", filtroAplicado = "") }
        obtenerTodos()
    }

    // ---------- Operaciones con el repositorio ----------

    fun obtenerTodos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            repository.obtenerTodos()
                .onSuccess { lista ->
                    _uiState.update { it.copy(isLoading = false, inquilinos = lista) }
                }
                .onFailure { error ->
                    mostrarError(error, "No se pudieron obtener los inquilinos.")
                }
        }
    }

    fun registrar(inquilino: Inquilino) {
        viewModelScope.launch {
            iniciarOperacion()

            repository.buscarPorDniCualquiera(inquilino.dni)
                .onSuccess { existente ->
                    when {
                        existente == null -> registrarNuevo(inquilino)

                        !existente.active -> _uiState.update {
                            it.copy(
                                isLoading = false,
                                inquilinoInactivoDetectado =
                                    inquilino.copy(idInquilino = existente.idInquilino)
                            )
                        }

                        else -> _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Ya existe un inquilino activo con el DNI ${inquilino.dni}."
                            )
                        }
                    }
                }
                .onFailure { error -> mostrarError(error, "Error al validar el DNI.") }
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
                .onSuccess { reactivado ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            inquilinos = it.inquilinos + reactivado,
                            successMessage = "Inquilino reactivado con éxito.",
                            mostrandoFormularioRegistro = false
                        )
                    }
                }
                .onFailure { error -> mostrarError(error, "No se pudo reactivar el inquilino.") }
        }
    }

    fun actualizar(inquilino: Inquilino) {
        viewModelScope.launch {
            iniciarOperacion()

            repository.actualizar(inquilino)
                .onSuccess { _ ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Datos del inquilino actualizados correctamente.",
                            inquilinoSeleccionado = null
                        )
                    }
                    obtenerTodos()
                }
                .onFailure { error -> mostrarError(error, "No se pudo actualizar el inquilino.") }
        }
    }

    fun darDeBaja(idInquilino: String) {
        viewModelScope.launch {
            iniciarOperacion()

            repository.darDeBaja(idInquilino)
                .onSuccess { _ ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Inquilino dado de baja correctamente.",
                            inquilinoDarDeBaja = null
                        )
                    }
                    obtenerTodos()
                }
                .onFailure { error -> mostrarError(error, "No se pudo dar de baja al inquilino.") }
        }
    }

    // ---------- Helpers privados ----------

    private suspend fun registrarNuevo(inquilino: Inquilino) {
        repository.registrar(inquilino)
            .onSuccess { nuevo ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        inquilinos = it.inquilinos + nuevo,
                        successMessage = "Inquilino registrado correctamente.",
                        mostrandoFormularioRegistro = false
                    )
                }
            }
            .onFailure { error -> mostrarError(error, "No se pudo registrar el inquilino.") }
    }

    private fun iniciarOperacion() =
        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

    private fun mostrarError(error: Exception, mensajePorDefecto: String) =
        _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: mensajePorDefecto) }
}