package com.example.gestionalquileres.ui.inmuebles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionalquileres.data.auth.repository.InmuebleRepository
import com.example.gestionalquileres.data.auth.repository.UnidadAlquilableRepository
import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Inmueble
import com.example.gestionalquileres.domain.model.UnidadAlquilable
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
class InmuebleViewModel @Inject constructor(
    private val repository: InmuebleRepository,
    private val unidadRepository: UnidadAlquilableRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InmuebleUiState())
    val uiState: StateFlow<InmuebleUiState> = _uiState.asStateFlow()

    // ------------------------------------------------------------------
    // Helpers privados
    // ------------------------------------------------------------------

    private fun iniciarOperacion() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
    }

    private fun fallo(mensaje: String?, porDefecto: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = mensaje ?: porDefecto) }
    }

    private fun cargarInmuebles(
        errorPorDefecto: String,
        consulta: suspend () -> AppResult<List<Inmueble>>
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            consulta()
                .onSuccess { lista -> _uiState.update { it.copy(isLoading = false, inmuebles = lista) } }
                .onFailure { error -> fallo(error.message, errorPorDefecto) }
        }
    }

    // ------------------------------------------------------------------
    // Lectura y búsqueda
    // ------------------------------------------------------------------

    fun obtenerTodos() =
        cargarInmuebles("No se pudieron obtener los inmuebles.") { repository.obtenerTodos() }

    fun buscar() {
        val estado = _uiState.value
        val texto = estado.textoBusqueda.trim()
        when {
            texto.isBlank() -> obtenerTodos()
            estado.tipoBusqueda == TipoBusqueda.DISTRITO ->
                cargarInmuebles("No se pudo buscar por distrito.") { repository.obtenerPorDistrito(texto) }
            else ->
                cargarInmuebles("No se pudo buscar por código postal.") { repository.obtenerPorCodigoPostal(texto) }
        }
    }

    fun mostrarTodos() {
        _uiState.update { it.copy(textoBusqueda = "") }
        obtenerTodos()
    }

    // ------------------------------------------------------------------
    // Altas, bajas y modificaciones
    // ------------------------------------------------------------------

    fun registrar(inmueble: Inmueble) {
        viewModelScope.launch {
            iniciarOperacion()
            repository.registrar(inmueble)
                .onSuccess { nuevo ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            inmuebles = it.inmuebles + nuevo,
                            mostrandoFormularioRegistro = false,
                            successMessage = "Inmueble registrado correctamente."
                        )
                    }
                }
                .onFailure { error -> fallo(error.message, "No se pudo registrar el inmueble.") }
        }
    }

    fun actualizar(inmueble: Inmueble) {
        viewModelScope.launch {
            iniciarOperacion()
            repository.actualizar(inmueble)
                .onSuccess { actualizado ->
                    _uiState.update { estado ->
                        estado.copy(
                            isLoading = false,
                            inmuebles = estado.inmuebles.map { if (it.idInmueble == actualizado.idInmueble) actualizado else it },
                            inmuebleSeleccionado = null,
                            successMessage = "Datos del inmueble actualizados correctamente."
                        )
                    }
                }
                .onFailure { error -> fallo(error.message, "No se pudo actualizar el inmueble.") }
        }
    }

    fun darDeBaja(idInmueble: String) {
        viewModelScope.launch {
            iniciarOperacion()
            repository.darDeBaja(idInmueble)
                .onSuccess {
                    _uiState.update { estado ->
                        estado.copy(
                            isLoading = false,
                            inmuebles = estado.inmuebles.filterNot { it.idInmueble == idInmueble },
                            inmuebleDarDeBaja = null,
                            successMessage = "Inmueble dado de baja correctamente."
                        )
                    }
                }
                .onFailure { error -> fallo(error.message, "No se pudo dar de baja al inmueble.") }
        }
    }

    fun reactivarInmueble(inmueble: Inmueble) {
        viewModelScope.launch {
            iniciarOperacion()
            _uiState.update { it.copy(inmuebleInactivoDetectado = null) }
            repository.reactivarYActualizar(inmueble)
                .onSuccess { reactivado ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            inmuebles = it.inmuebles + reactivado,
                            successMessage = "Inmueble reactivado con éxito."
                        )
                    }
                }
                .onFailure { error -> fallo(error.message, "No se pudo reactivar el inmueble.") }
        }
    }

    // ------------------------------------------------------------------
    // Detalle: unidades alquilables
    // ------------------------------------------------------------------

    fun verDetalle(inmueble: Inmueble) {
        _uiState.update {
            it.copy(inmuebleEnDetalle = inmueble, unidadesAlquilables = emptyList(), cargandoUnidades = true)
        }
        viewModelScope.launch {
            unidadRepository.obtenerUnidadesPorInmueble(inmueble.idInmueble)
                .onSuccess { unidades ->
                    _uiState.update {
                        it.copy(cargandoUnidades = false, unidadesAlquilables = unidades.ordenadas())
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            cargandoUnidades = false,
                            errorMessage = error.message ?: "No se pudieron obtener las unidades alquilables."
                        )
                    }
                }
        }
    }

    fun actualizarPrecioUnidad(inmuebleId: String, unidadId: String, nuevoPrecio: Double) {
        viewModelScope.launch {
            unidadRepository.actualizarPrecioRenta(inmuebleId, unidadId, nuevoPrecio)
                .onSuccess {
                    _uiState.update { estado ->
                        estado.copy(
                            unidadesAlquilables = estado.unidadesAlquilables.map { unidad ->
                                if (unidad.id == unidadId) unidad.copy(precioRenta = nuevoPrecio) else unidad
                            }
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "No se pudo actualizar el precio.") }
                }
        }
    }

    private fun List<UnidadAlquilable>.ordenadas() =
        sortedWith(compareBy<UnidadAlquilable>({ it.piso }, { it.numeroUnidad }))

    // ------------------------------------------------------------------
    // Estado de la interfaz
    // ------------------------------------------------------------------

    fun mostrarFormularioRegistro(mostrar: Boolean) {
        _uiState.update {
            it.copy(mostrandoFormularioRegistro = mostrar, errorMessage = null, successMessage = null)
        }
    }

    fun seleccionarInmuebleParaEditar(inmueble: Inmueble?) {
        _uiState.update { it.copy(inmuebleSeleccionado = inmueble) }
    }

    fun seleccionarInmuebleParaBaja(inmueble: Inmueble?) {
        _uiState.update { it.copy(inmuebleDarDeBaja = inmueble) }
    }

    /** Cierra formulario, edición o detalle y vuelve al listado. */
    fun volverAlListado() {
        _uiState.update {
            it.copy(
                mostrandoFormularioRegistro = false,
                inmuebleSeleccionado = null,
                inmuebleEnDetalle = null,
                unidadesAlquilables = emptyList()
            )
        }
    }

    fun cancelarReactivacion() {
        _uiState.update { it.copy(inmuebleInactivoDetectado = null, errorMessage = null) }
    }

    fun actualizarTipoBusqueda(tipo: TipoBusqueda) {
        _uiState.update { it.copy(tipoBusqueda = tipo) }
    }

    fun actualizarTextoBusqueda(texto: String) {
        _uiState.update { it.copy(textoBusqueda = texto) }
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}