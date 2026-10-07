package com.example.gestionalquileres.ui.inmuebles

import com.example.gestionalquileres.domain.model.Inmueble
import com.example.gestionalquileres.domain.model.UnidadAlquilable

data class InmuebleUiState(
    val isLoading: Boolean = false,
    val inmuebles: List<Inmueble> = emptyList(),
    val inmuebleSeleccionado: Inmueble? = null,
    val inmuebleInactivoDetectado: Inmueble? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val numeroPisos: String = "",                              // Para capturar el texto en el formulario de registro
    val unidadesPorPiso: String = "",                          // Para capturar el texto en el formulario de registro
    val unidadesAlquilables: List<UnidadAlquilable> = emptyList() // Para mostrar la lista de unidades de un inmueble seleccionado
)