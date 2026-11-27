package com.example.gestionalquileres.ui.inquilinos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionalquileres.domain.model.FotoRepository
import com.example.gestionalquileres.data.repository.InquilinoRepositoryImpl
import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Inquilino
import com.example.gestionalquileres.domain.model.TipoFoto
import com.example.gestionalquileres.domain.model.onFailure
import com.example.gestionalquileres.domain.model.onSuccess
import com.example.gestionalquileres.domain.model.sincronizar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InquilinoViewModel @Inject constructor(
    private val repository: InquilinoRepositoryImpl,
    private val fotoRepository: FotoRepository
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

    fun limpiarInquilinoSeleccionado() =
        _uiState.update { it.copy(inquilinoSeleccionado = null) }

    // ---------- Buscador ----------

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
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            inquilinos = lista.distinctBy { item -> item.idInquilino }
                        )
                    }
                }
                .onFailure { error ->
                    mostrarError(error, "No se pudieron obtener los inquilinos.")
                }
        }
    }

    fun obtenerPorDni(dni: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

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
                    mostrarError(error, "No se pudo buscar por DNI.")
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
                    mostrarError(error, "No se pudo buscar por nombre.")
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
                    val fotoOk = fotoRepository.sincronizar(
                        TipoFoto.INQUILINO, reactivado.idInquilino,
                        reactivado.fotografia, inquilino.fotoCompleta, teniaFoto = true
                    )
                    _uiState.update { estado ->
                        val listaActualizada = (estado.inquilinos + reactivado.copy(fotoCompleta = null))
                            .distinctBy { it.idInquilino }
                        estado.copy(
                            isLoading = false,
                            inquilinos = listaActualizada,
                            successMessage = mensajeConFoto("Inquilino reactivado con éxito.", fotoOk),
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

            val teniaFoto = _uiState.value.inquilinos
                .firstOrNull { it.idInquilino == inquilino.idInquilino }?.fotografia != null

            repository.actualizar(inquilino)
                .onSuccess { actualizado ->
                    val fotoOk = fotoRepository.sincronizar(
                        TipoFoto.INQUILINO, actualizado.idInquilino,
                        actualizado.fotografia, inquilino.fotoCompleta, teniaFoto
                    )
                    _uiState.update { estado ->
                        estado.copy(
                            isLoading = false,
                            inquilinos = estado.inquilinos.map {
                                if (it.idInquilino == actualizado.idInquilino) {
                                    actualizado.copy(fotoCompleta = null)
                                } else {
                                    it
                                }
                            }.distinctBy { it.idInquilino },
                            successMessage = mensajeConFoto("Datos del inquilino actualizados correctamente.", fotoOk),
                            inquilinoSeleccionado = null
                        )
                    }
                }
                .onFailure { error -> mostrarError(error, "No se pudo actualizar el inquilino.") }
        }
    }

    fun darDeBaja(idInquilino: String) {
        viewModelScope.launch {
            iniciarOperacion()
            ejecutarBaja(idInquilino)
        }
    }

    // ---------- Helpers privados ----------

    private fun mensajeConFoto(base: String, fotoOk: Boolean) =
        if (fotoOk) base else "$base Sin embargo, no se pudo guardar la foto en alta calidad."

    private suspend fun registrarNuevo(inquilino: Inquilino) {
        repository.registrar(inquilino)
            .onSuccess { nuevo ->
                val fotoOk = fotoRepository.sincronizar(
                    TipoFoto.INQUILINO, nuevo.idInquilino,
                    inquilino.fotografia, inquilino.fotoCompleta, teniaFoto = false
                )
                _uiState.update { estado ->
                    val listaActualizada = (estado.inquilinos + nuevo.copy(fotoCompleta = null))
                        .distinctBy { it.idInquilino }
                    estado.copy(
                        isLoading = false,
                        inquilinos = listaActualizada,
                        successMessage = mensajeConFoto("Inquilino registrado correctamente.", fotoOk),
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

    private suspend fun ejecutarBaja(idInquilino: String) {
        repository.darDeBaja(idInquilino)
            .onSuccess {
                _uiState.update { estado ->
                    estado.copy(
                        isLoading = false,
                        inquilinos = estado.inquilinos.filter { it.idInquilino != idInquilino },
                        successMessage = "Inquilino dado de baja correctamente.",
                        inquilinoDarDeBaja = null
                    )
                }
            }
            .onFailure { error -> bajaRechazada(error.message ?: "No se pudo dar de baja al inquilino.") }
    }

    /** Cierra el diálogo de baja y muestra el motivo en el Snackbar. */
    private fun bajaRechazada(mensaje: String) {
        _uiState.update { it.copy(isLoading = false, inquilinoDarDeBaja = null, errorMessage = mensaje) }
    }

    fun cargarFotoCompleta(tipo: TipoFoto, idPropietario: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val resultado = fotoRepository.obtener(tipo, idPropietario)) {
                is AppResult.Exito -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        fotoCompletaEnVista = resultado.valor
                    )
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = resultado.excepcion.message ?: "No se pudo cargar la foto."
                    )
                }
            }
        }
    }

    fun alSeleccionarFotoMiniatura(inquilino: Inquilino) {
        cargarFotoCompleta(
            tipo = TipoFoto.INQUILINO,
            idPropietario = inquilino.idInquilino
        )
    }
}