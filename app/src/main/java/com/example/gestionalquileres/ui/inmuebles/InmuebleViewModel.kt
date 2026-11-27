package com.example.gestionalquileres.ui.inmuebles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionalquileres.data.repository.InmuebleRepositoryImpl
import com.example.gestionalquileres.data.repository.UnidadAlquilableRepositoryImpl
import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.DISTRITOS_LIMA
import com.example.gestionalquileres.domain.model.FotoRepository
import com.example.gestionalquileres.domain.model.Inmueble
import com.example.gestionalquileres.domain.model.TipoFoto
import com.example.gestionalquileres.domain.model.UnidadAlquilable
import com.example.gestionalquileres.domain.model.claveDireccion
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
class InmuebleViewModel @Inject constructor(
    private val repository: InmuebleRepositoryImpl,
    private val unidadRepository: UnidadAlquilableRepositoryImpl,
    private val fotoRepository: FotoRepository,
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

    private fun mensajeConFoto(base: String, fotoOk: Boolean) =
        if (fotoOk) base else "$base Sin embargo, no se pudo guardar la foto en alta calidad."

    // ------------------------------------------------------------------
    // Carga y paginación
    // ------------------------------------------------------------------

    /** Carga la primera página (reinicia la lista y el cursor). */
    fun cargarPrimeraPagina() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.obtenerPagina()
                .onSuccess { pagina ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            inmuebles = pagina.items.distinctBy { item -> item.idInmueble },
                            siguientePagina = pagina.siguiente
                        )
                    }
                }
                .onFailure { error -> fallo(error.message, "No se pudieron obtener los inmuebles.") }
        }
    }

    /** Alias compatible si alguna pantalla llama a obtenerTodos(). */
    fun obtenerTodos() = cargarPrimeraPagina()

    /** Carga la siguiente página y la agrega al final. */
    fun cargarMas() {
        val cursor = _uiState.value.siguientePagina ?: return
        if (_uiState.value.cargandoMas) return
        _uiState.update { it.copy(cargandoMas = true) }

        viewModelScope.launch {
            repository.obtenerPagina(despues = cursor)
                .onSuccess { pagina ->
                    _uiState.update { estado ->
                        estado.copy(
                            cargandoMas = false,
                            inmuebles = (estado.inmuebles + pagina.items).distinctBy { it.idInmueble },
                            siguientePagina = pagina.siguiente
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            cargandoMas = false,
                            errorMessage = error.message ?: "No se pudieron cargar más inmuebles."
                        )
                    }
                }
        }
    }

    /** Para buscar en memoria hay que tener todos los registros: trae las páginas que faltan. */
    private fun cargarTodasLasPaginas() {
        if (_uiState.value.siguientePagina == null || _uiState.value.cargandoMas) return
        _uiState.update { it.copy(cargandoMas = true) }

        viewModelScope.launch {
            var cursor = _uiState.value.siguientePagina
            while (cursor != null) {
                when (val resultado = repository.obtenerPagina(despues = cursor)) {
                    is AppResult.Exito -> {
                        val pagina = resultado.valor
                        _uiState.update { estado ->
                            estado.copy(
                                inmuebles = (estado.inmuebles + pagina.items).distinctBy { it.idInmueble },
                                siguientePagina = pagina.siguiente
                            )
                        }
                        cursor = pagina.siguiente
                    }
                    is AppResult.Error -> {
                        _uiState.update {
                            it.copy(
                                errorMessage = resultado.excepcion.message
                                    ?: "No se pudieron cargar todos los inmuebles."
                            )
                        }
                        cursor = null
                    }
                }
            }
            _uiState.update { it.copy(cargandoMas = false) }
        }
    }

    // ------------------------------------------------------------------
    // Lectura y búsqueda
    // ------------------------------------------------------------------

    fun buscar() {
        val texto = _uiState.value.textoBusqueda.trim()
        _uiState.update { it.copy(filtroAplicado = texto, tipoFiltroAplicado = it.tipoBusqueda) }
        if (texto.isNotEmpty()) cargarTodasLasPaginas()
    }

    fun mostrarTodos() {
        _uiState.update { it.copy(textoBusqueda = "", filtroAplicado = "") }
        cargarPrimeraPagina()
    }

    // ------------------------------------------------------------------
    // Altas, bajas y modificaciones
    // ------------------------------------------------------------------

    fun registrar(inmueble: Inmueble) {
        viewModelScope.launch {
            iniciarOperacion()

            if (inmueble.distrito !in DISTRITOS_LIMA) {
                fallo(MENSAJE_DISTRITO_INVALIDO, MENSAJE_DISTRITO_INVALIDO)
                return@launch
            }

            repository.buscarPorDireccion(
                inmueble.distrito, inmueble.direccion, inmueble.numeroDireccion
            )
                .onFailure { error -> fallo(error.message, "No se pudo verificar si el inmueble ya existe.") }
                .onSuccess { existente ->
                    when {
                        existente == null -> registrarNuevo(inmueble)

                        !existente.active -> _uiState.update {
                            it.copy(
                                isLoading = false,
                                inmuebleInactivoDetectado = existente.conDatosDe(inmueble)
                            )
                        }

                        else -> fallo(MENSAJE_DUPLICADO, MENSAJE_DUPLICADO)
                    }
                }
        }
    }

    private suspend fun registrarNuevo(inmueble: Inmueble) {
        repository.registrar(inmueble)
            .onSuccess { nuevo ->
                val fotoOk = fotoRepository.sincronizar(
                    TipoFoto.INMUEBLE, nuevo.idInmueble,
                    inmueble.fotografia, inmueble.fotoCompleta, teniaFoto = false
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        inmuebles = (it.inmuebles + nuevo.copy(fotoCompleta = null))
                            .distinctBy { item -> item.idInmueble },
                        mostrandoFormularioRegistro = false,
                        successMessage = mensajeConFoto("Inmueble registrado correctamente.", fotoOk)
                    )
                }
            }
            .onFailure { error -> fallo(error.message, "No se pudo registrar el inmueble.") }
    }

    private fun Inmueble.conDatosDe(nuevo: Inmueble) = copy(
        distrito = nuevo.distrito,
        direccion = nuevo.direccion,
        numeroDireccion = nuevo.numeroDireccion,
        codigoPostal = nuevo.codigoPostal,
        fotografia = nuevo.fotografia ?: fotografia,
        fotoCompleta = nuevo.fotoCompleta
    )

    fun actualizar(inmueble: Inmueble) {
        viewModelScope.launch {
            iniciarOperacion()

            if (inmueble.distrito !in DISTRITOS_LIMA) {
                fallo(MENSAJE_DISTRITO_INVALIDO, MENSAJE_DISTRITO_INVALIDO)
                return@launch
            }

            val original = _uiState.value.inmuebles.firstOrNull { it.idInmueble == inmueble.idInmueble }
            if (original != null && original.claveDireccion == inmueble.claveDireccion) {
                guardarCambios(inmueble)
                return@launch
            }

            repository.buscarPorDireccion(
                inmueble.distrito, inmueble.direccion, inmueble.numeroDireccion,
                excluirId = inmueble.idInmueble
            )
                .onFailure { error -> fallo(error.message, "No se pudo verificar la dirección.") }
                .onSuccess { otro ->
                    when {
                        otro == null -> guardarCambios(inmueble)
                        otro.active -> fallo(MENSAJE_DUPLICADO, MENSAJE_DUPLICADO)
                        else -> fallo(MENSAJE_DUPLICADO_DE_BAJA, MENSAJE_DUPLICADO_DE_BAJA)
                    }
                }
        }
    }

    private suspend fun guardarCambios(inmueble: Inmueble) {
        val teniaFoto = _uiState.value.inmuebles
            .firstOrNull { it.idInmueble == inmueble.idInmueble }?.fotografia != null

        repository.actualizar(inmueble)
            .onSuccess { actualizado ->
                val fotoOk = fotoRepository.sincronizar(
                    TipoFoto.INMUEBLE, actualizado.idInmueble,
                    actualizado.fotografia, inmueble.fotoCompleta, teniaFoto
                )
                _uiState.update { estado ->
                    estado.copy(
                        isLoading = false,
                        inmuebles = estado.inmuebles.map {
                            if (it.idInmueble == actualizado.idInmueble) {
                                actualizado.copy(fotoCompleta = null)
                            } else {
                                it
                            }
                        }.distinctBy { it.idInmueble },
                        inmuebleSeleccionado = null,
                        successMessage = mensajeConFoto("Datos del inmueble actualizados correctamente.", fotoOk)
                    )
                }
            }
            .onFailure { error -> fallo(error.message, "No se pudo actualizar el inmueble.") }
    }

    fun darDeBaja(idInmueble: String) {
        viewModelScope.launch {
            iniciarOperacion()
            ejecutarBaja(idInmueble)
        }
    }

    private suspend fun ejecutarBaja(idInmueble: String) {
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
            .onFailure { error -> bajaRechazada(error.message ?: "No se pudo dar de baja al inmueble.") }
    }

    private fun bajaRechazada(mensaje: String) {
        _uiState.update { it.copy(isLoading = false, inmuebleDarDeBaja = null, errorMessage = mensaje) }
    }

    fun reactivarInmueble(inmueble: Inmueble) {
        viewModelScope.launch {
            iniciarOperacion()
            _uiState.update { it.copy(inmuebleInactivoDetectado = null) }
            repository.reactivarYActualizar(inmueble)
                .onSuccess { reactivado ->
                    val fotoOk = fotoRepository.sincronizar(
                        TipoFoto.INMUEBLE, reactivado.idInmueble,
                        reactivado.fotografia, inmueble.fotoCompleta, teniaFoto = false
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            inmuebles = (it.inmuebles + reactivado.copy(fotoCompleta = null))
                                .distinctBy { item -> item.idInmueble },
                            mostrandoFormularioRegistro = false,
                            successMessage = mensajeConFoto("Inmueble reactivado con éxito.", fotoOk)
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

    private companion object {
        const val MENSAJE_DUPLICADO = "Ya existe un inmueble registrado en esa dirección."

        const val MENSAJE_DUPLICADO_DE_BAJA =
            "Esa dirección pertenece a un inmueble dado de baja. " +
                    "Usa otra dirección o reactívalo desde \"Registrar\"."

        private const val MENSAJE_DISTRITO_INVALIDO = "Selecciona un distrito válido de Lima."
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

    fun alSeleccionarFotoMiniatura(inmueble: Inmueble) {
        cargarFotoCompleta(
            tipo = TipoFoto.INMUEBLE,
            idPropietario = inmueble.idInmueble // Ajusta si tu campo se llama distinto, ej. inmueble.id
        )
    }
}