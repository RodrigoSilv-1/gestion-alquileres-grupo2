package com.example.gestionalquileres.ui.recibos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionalquileres.data.repository.ContratoRepository
import com.example.gestionalquileres.data.repository.ReciboRepository
import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Contrato
import com.example.gestionalquileres.domain.model.Recibo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ReciboViewModel @Inject constructor(
    private val reciboRepository: ReciboRepository,
    private val contratoRepository: ContratoRepository // Inyectado para traer los contratos activos
) : ViewModel() {

    // (Aquí iría tu StateFlow tradicional, resumido para el ejemplo)
    private val _uiState = MutableStateFlow(ReciboUiState())
    val uiState: StateFlow<ReciboUiState> = _uiState.asStateFlow()

    fun cargarContratosParaCobro() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // Evaluamos la respuesta que nos devolvió el repositorio al ir a buscar los contratos activos
            when (val resultado = contratoRepository.obtenerContratosActivos()) {

                // Si la operación salió bien (Éxito)
                is AppResult.Exito -> {
                    // Actualizamos los datos de la pantalla
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,              // Apagamos el indicador de carga (el círculo girando)
                        contratosActivos = resultado.valor // Guardamos la lista de contratos reales que nos trajo el servidor
                    )
                }

                // Si la operación falló (Error por falta de internet, problemas de base de datos, etc.)
                is AppResult.Error -> {
                    // Actualizamos la pantalla para mostrar el problema
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,              // Apagamos la carga para que la pantalla no quede bloqueada
                        errorMessage = "No se pudieron cargar los contratos." // Mensaje de aviso para el usuario
                    )
                }
            }
        }
    }

    // Proceso 4 y 5: Procesar datos del formulario y enviar a Firestore
    fun generarRecibo(
        contratoSeleccionado: Contrato,
        inputAgua: String,
        inputLuz: String,
        inputMantenimiento: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // 1. Convertimos los textos ingresados a números de forma segura.
            // Si el campo está vacío o mal escrito, evitamos que la app se rompa usando 0.0 por defecto (Operador Elvis)
            val agua = inputAgua.toDoubleOrNull() ?: 0.0
            val luz = inputLuz.toDoubleOrNull() ?: 0.0
            val mantenimiento = inputMantenimiento.toDoubleOrNull() ?: 0.0
            val renta = contratoSeleccionado.precioRentaAcordado

            // 2. Cálculos (Criterio 4)
            val igv = renta * 0.18
            val total = renta + igv + agua + luz + mantenimiento

            //Creamos un formato que define cómo queremos que se escriba la fecha (Año-Mes-Día)
            val formatoFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            //obtenemos la fecha de hoy
            // para guardarla como un texto 'fechaHoy'
            val fechaHoy = formatoFecha.format(Date())

            // Construimos el objeto 'Recibo' con todos los datos listos para ser guardados
            val reciboParcial = Recibo(
                idContrato = contratoSeleccionado.idContrato,
                nombreInquilino = contratoSeleccionado.nombreInquilino,
                dniInquilino = contratoSeleccionado.dniInquilino,
                direccionInmueble = contratoSeleccionado.direccionInmueble,
                distritoInmueble = contratoSeleccionado.distritoInmueble,
                nombreUnidad = contratoSeleccionado.nombreUnidad,
                fechaEmision = fechaHoy,
                rentaBase = renta,
                montoIgv = igv,
                agua = agua,
                luz = luz,
                mantenimiento = mantenimiento,
                montoTotal = total,
                estadoPago = Recibo.ESTADO_PENDIENTE
            )

            // 4. Delegar al repositorio para crear el correlativo y guardar
            when (val resultado = reciboRepository.generarYGuardarRecibo(reciboParcial)) {
                is AppResult.Exito -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        recibos = listOf(resultado.valor) + _uiState.value.recibos,
                        successMessage = "Recibo ${resultado.valor.numeroUnico} generado correctamente."
                    )
                }
                is AppResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = resultado.excepcion.message ?: "Error al generar recibo."
                    )
                }
            }
        }
    }

    fun cargarHistorialRecibos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            when (val resultado = reciboRepository.obtenerTodosLosRecibos()) {
                is AppResult.Exito -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        recibos = resultado.valor
                    )
                }
                is AppResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = resultado.excepcion.message ?: "Error al cargar historial."
                    )
                }
            }
        }
    }

    fun cambiarEstadoPago(recibo: Recibo) {
        // 1. Determinamos el nuevo estado: si actualmente está "Cobrado", pasa a "Pendiente"; de lo contrario, pasa a "Cobrado" (efecto interruptor/toggle)
        val nuevoEstado =
            if (recibo.estadoPago == Recibo.ESTADO_COBRADO) {
                Recibo.ESTADO_PENDIENTE
            } else {
                Recibo.ESTADO_COBRADO
            }

// 2. Lanzamos una corrutina en el hilo del ViewModel para manejar la operación de forma asíncrona
        viewModelScope.launch {
            // Activamos el indicador de carga (spinner) en la interfaz y limpiamos cualquier mensaje anterior de éxito o error
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            // 3. Ejecutamos la función del repositorio para actualizar el estado del pago en la base de datos
            when (
                val resultado = reciboRepository.actualizarEstadoPago(
                    idRecibo = recibo.idRecibo,
                    nuevoEstado = nuevoEstado
                )
            ) {
                // Caso A: La actualización en la base de datos fue exitosa
                is AppResult.Exito -> {
                    // Actualizamos la lista local de recibos recorriéndola: si coincide el ID, cambiamos su estado de pago; si no, lo dejamos igual
                    val recibosActualizados = _uiState.value.recibos.map { actual ->
                        if (actual.idRecibo == recibo.idRecibo) {
                            actual.copy(estadoPago = nuevoEstado)
                        } else {
                            actual
                        }
                    }

                    // Actualizamos el estado de la UI: desactivamos la carga, pasamos la lista actualizada y mostramos un mensaje de éxito
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        recibos = recibosActualizados,
                        successMessage = "Recibo marcado como $nuevoEstado."
                    )
                }

                // Caso B: Ocurrió un error al intentar actualizar el estado
                is AppResult.Error -> {
                    // Actualizamos la UI: desactivamos la carga y guardamos el mensaje de error (o uno por defecto si la excepción no trae mensaje)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = resultado.excepcion.message
                            ?: "No se pudo actualizar el estado del recibo."
                    )
                }
            }
        }
    }

    // --- FUNCIONES DE ESTADO DE LA INTERFAZ (UI STATE) ---

    fun mostrarFormularioRegistro(mostrar: Boolean) {
        _uiState.value = _uiState.value.copy(mostrandoFormularioRegistro = mostrar)
    }

    fun seleccionarRecibo(recibo: Recibo?) {
        _uiState.value = _uiState.value.copy(reciboSeleccionado = recibo)
    }

    fun actualizarTipoBusqueda(tipo: String) {
        _uiState.value = _uiState.value.copy(tipoBusqueda = tipo)
    }

    fun actualizarTextoBusqueda(texto: String) {
        _uiState.value = _uiState.value.copy(textoBusqueda = texto)
    }

    // Función para actualizar el filtro de estado actual (por ejemplo: Todos, Pendientes o Cobrados)
    fun actualizarFiltroEstado(filtro: String) {
        // 1. Definimos un conjunto cerrado de filtros válidos permitidos por la interfaz/modelo
        val filtrosPermitidos = setOf(
            ReciboUiState.FILTRO_TODOS,
            ReciboUiState.FILTRO_PENDIENTES,
            ReciboUiState.FILTRO_COBRADOS
        )

        // 2. Validamos que el filtro recibido esté estrictamente dentro de los permitidos por seguridad
        if (filtro in filtrosPermitidos) {
            // 3. Si es válido, actualizamos el estado de la UI copiando el valor actual y modificando solo el filtro de estado
            _uiState.value = _uiState.value.copy(
                filtroEstado = filtro
            )
        }
    }

    fun limpiarMensajes() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}
