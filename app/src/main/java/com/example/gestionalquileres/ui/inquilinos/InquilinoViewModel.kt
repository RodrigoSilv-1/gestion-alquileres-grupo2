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
        //Aquí iniciamos una corrutina con viewModelScope.launch para que
        //todo este proceso de registro ocurra de forma asíncrona en segundo plano
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,//activar la bandera de carga y limpiando cualquier mensaje de error
                successMessage = null,// o éxito previo para preparar la interfaz."
                errorMessage = null,
            )

            repository.buscarPorDniCualquiera(inquilino.dni)//llamamos al repositorio para consultar en la bd
                .onSuccess { inquilinoExistente ->
                    // Evaluamos si se encontró un inquilino con ese DNI en la base de datos
                    if (inquilinoExistente != null) {
                        // Apagamos el indicador de carga
                        _uiState.value = _uiState.value.copy(isLoading = false)

                        // Evaluamos si el inquilino encontrado está INACTIVO (!active)
                        if (!inquilinoExistente.active) {
                            // creamos una copia del inquilino que ibamos a registrar agregandole el id del inquilino viejo
                            val inquilinoParaReactivar = inquilino.copy(
                                idInquilino = inquilinoExistente.idInquilino
                            )
                            // Cambiamos el estado global de la app para que la pantalla detecte al
                            // inquilino inactivo y lance la ventana emergente
                            _uiState.value = _uiState.value.copy(
                                inquilinoInactivoDetectado = inquilinoParaReactivar
                            )
                        } else {
                            // Si el inquilino YA está activo, bloqueamos el registro y mostramos un error de DNI duplicado
                            _uiState.value = _uiState.value.copy(
                                errorMessage = "Ya existe un inquilino activo con el DNI ${inquilino.dni}."
                            )
                        }
                    } else {
                        // Si NO se encontró ningún registro con ese DNI, procedemos a crear un registro totalmente nuevo
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
        // 1. Lanzamos una corrutina en segundo plano desde el ViewModel
        viewModelScope.launch {

            // 2. Ponemos la app en modo carga antes de ir por los datos a la nube
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            // 3. Llamamos al repositorio, el cual nos devuelve un contenedor 'AppResult'
            //    (que por dentro trae o un 'Exito' con la lista o un 'Error' con la excepción).
            repository.obtenerTodos()

                // 4. .onSuccess SE CONECTA con el AppResult.Exito:
                //    Si Firebase respondió bien, este bloque se activa y recibe la 'lista'.
                .onSuccess { lista ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,          // Apagamos el indicador de carga
                        inquilinos = lista          // Guardamos los datos en el estado de la UI
                    )
                }

                // 5. .onFailure SE CONECTA con el AppResult.Error:
                //    Si hubo un fallo de red o Firestore rechazó la petición, se activa este bloque.
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,          // Apagamos la carga para no dejar la pantalla trabada
                        errorMessage = error.message ?: "No se pudieron obtener los inquilinos." // Guardamos el error
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
        // Lanzamos una corrutina asociada al ciclo de vida del ViewModel para ejecutar tareas asíncronas en segundo plano
        viewModelScope.launch {
            // actualizamos el estado de ui y activamos el indicador de carga
            _uiState.value = _uiState.value.copy(
                isLoading = true,           // Enciende el indicador de carga (spinner) y bloquea la interfaz
                errorMessage = null,        // Borra cualquier error anterior
                successMessage = null       // Borra cualquier mensaje de éxito anterior
            )
            // Llamamos al repositorio para actualizar los datos del inquilino en Firebase
            repository.actualizar(inquilino)

                // Si la actualización en la nube se ejecuta correctamente
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,                  // Apagamos la carga
                        successMessage = "Datos del inquilino actualizados correctamente.", // Mensaje de confirmación
                        inquilinoSeleccionado = null        // Limpiamos el inquilino seleccionado para cerrar la ventana/modal de edición
                    )
                    // Volvemos a consultar la lista completa para que la interfaz se refresque con los datos nuevos
                    obtenerTodos()
                }

                // Si ocurre un fallo de red o de Firestore durante la actualización
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,                  // Apagamos la carga para no dejar la pantalla bloqueada
                        errorMessage = error.message        // Guardamos el mensaje original del error
                            ?: "No se pudo actualizar el inquilino." // O usamos un texto por defecto con el operador Elvis si viene nulo
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
            // Si el registro en Firebase sale bien, recibimos el objeto ya creado con su ID
            .onSuccess { nuevoInquilino ->

                // Creamos una nueva lista sumando la lista actual más el nuevo inquilino registrado (aplicando inmutabilidad en Compose)
                val listaActualizada = _uiState.value.inquilinos + nuevoInquilino

                // Actualizamos el estado de la UI con los nuevos cambios
                _uiState.value = _uiState.value.copy(
                    isLoading = false,                      // Apagamos el indicador de carga
                    inquilinos = listaActualizada,          // Actualizamos la lista visible añadiendo el inquilino recién creado
                    successMessage = "Inquilino registrado correctamente.", // Mensaje de éxito para el usuario
                    mostrandoFormularioRegistro = false     // Cierra el formulario automáticamente tras el registro exitoso
                )
            }
            .onFailure { error ->
                // Actualizamos el estado para reflejar el fallo en la interfaz
                _uiState.value = _uiState.value.copy(
                    isLoading = false,                      // Apagamos la carga para no dejar la pantalla bloqueada
                    errorMessage = error.message ?: "No se pudo registrar el inquilino." // Mensaje de error por defecto usando el operador Elvis
                )
            }
    }

    fun reactivarInquilino(inquilino: Inquilino) {
        viewModelScope.launch {
            // Actualizamos el estado de la UI con los cambios en modo de carga
            _uiState.value = _uiState.value.copy(
                isLoading = true,                    // Activamos el indicador de carga (spinner) y bloqueamos acciones repetidas
                errorMessage = null,                 // Limpiamos cualquier error previo para que la pantalla quede "limpia"
                successMessage = null,               // Limpiamos mensajes de éxito anteriores para que no se queden estáticos
                inquilinoInactivoDetectado = null    // Reseteamos el estado de inactividad para cerrar o evitar diálogos pendientes
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