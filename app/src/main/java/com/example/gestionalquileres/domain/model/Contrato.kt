package com.example.gestionalquileres.domain.model

data class Contrato(
    val idContrato: String = "",
    val inmuebleId: String = "",
    val direccionInmueble: String = "",   // Dirección del edificio (ej. "Av. Los Pinos 123")
    val distritoInmueble: String = "",   // Distrito del edificio
    val unidadId: String = "",
    val nombreUnidad: String = "",       // Nombre o número de la unidad (ej. "Unidad 201")
    val inquilinoId: String = "",
    val nombreInquilino: String = "",    // Nombre completo del inquilino
    val dniInquilino: String = "",       // DNI del inquilino
    val precioRentaAcordado: Double = 0.0, // Monto de la renta acordada
    val fechaInicio: String = "",        // Fecha de inicio del contrato
    val fechaFin: String = "",           // Fecha de término del contrato
    val active: Boolean = true           // True mientras esté vigente
)