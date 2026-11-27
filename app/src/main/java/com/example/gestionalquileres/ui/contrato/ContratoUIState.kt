package com.example.gestionalquileres.ui.contrato

import com.example.gestionalquileres.domain.model.Contrato
import com.example.gestionalquileres.domain.model.CursorPagina
import com.example.gestionalquileres.domain.model.Inmueble
import com.example.gestionalquileres.domain.model.Inquilino
import com.example.gestionalquileres.domain.model.UnidadAlquilable

data class ContratoUiState(
    // Lista paginada que ve la pantalla
    val contratosActivos: List<Contrato> = emptyList(),
    val siguientePagina: CursorPagina? = null,
    val cargandoMas: Boolean = false,

    // Solo para el formulario (todos los contratos vigentes, se cargan al abrirlo)
    val contratosParaFormulario: List<Contrato> = emptyList(),

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
) {
    val hayMas: Boolean get() = siguientePagina != null

    /** Inquilinos sin contrato vigente: son los únicos que tiene sentido ofrecer en el selector. */
    val inquilinosDisponibles: List<Inquilino>
        get() {
            val conContrato = contratosParaFormulario.mapTo(HashSet()) { it.inquilinoId }   // <-- antes: contratosActivos
            return listaInquilinos.filter { it.idInquilino !in conContrato }
        }
}