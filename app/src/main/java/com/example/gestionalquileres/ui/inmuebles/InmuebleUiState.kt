package com.example.gestionalquileres.ui.inmuebles

import com.example.gestionalquileres.domain.model.Inmueble
import com.example.gestionalquileres.domain.model.UnidadAlquilable

data class InmuebleUiState(
    val inmuebles: List<Inmueble> = emptyList(),
    val isLoading: Boolean = false,

    // Control de ventanas y formularios
    val mostrandoFormularioRegistro: Boolean = false,
    val inmuebleSeleccionado: Inmueble? = null, // Para editar
    val inmuebleDarDeBaja: Inmueble? = null, // Para la alerta de eliminar
    val inmuebleInactivoDetectado: Inmueble? = null, // Para reactivar

    // Buscador
    val tipoBusqueda: String = "Distrito",
    val textoBusqueda: String = "",

    // Mensajes
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val numeroPisos: String = "",                              // Para capturar el texto en el formulario de registro
    val unidadesPorPiso: String = "",                          // Para capturar el texto en el formulario de registro
    val unidadesAlquilables: List<UnidadAlquilable> = emptyList() // Para mostrar la lista de unidades de un inmueble seleccionado
)