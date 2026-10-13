package com.example.gestionalquileres.ui.contrato

import com.example.gestionalquileres.domain.model.Contrato
import com.example.gestionalquileres.domain.model.Inquilino
import com.example.gestionalquileres.domain.model.Inmueble
import com.example.gestionalquileres.domain.model.UnidadAlquilable

data class ContratoUiState(
    val contratosActivos: List<Contrato> = emptyList(),
    val historialContratos: List<Contrato> = emptyList(),
    val listaInquilinos: List<Inquilino> = emptyList(),

    // Lista directa de pares (Inmueble, UnidadAlquilable) libres y con renta válida
    val listaUnidadesDisponibles: List<Pair<Inmueble, UnidadAlquilable>> = emptyList(),

    val inquilinoSeleccionado: Inquilino? = null,
    val inmuebleSeleccionado: Inmueble? = null,
    val unidadSeleccionada: UnidadAlquilable? = null,
    val fechaInicio: String = "",
    val fechaFin: String = "",
    val esSolvente: Boolean? = null,
    val mensajeSolvencia: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)