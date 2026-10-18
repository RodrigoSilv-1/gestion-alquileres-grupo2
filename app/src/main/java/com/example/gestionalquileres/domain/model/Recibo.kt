package com.example.gestionalquileres.domain.model

data class Recibo(
    val idRecibo: String = "",
    val nombreInquilino: String = "",
    val dniInquilino: String = "",
    val direccionInmueble: String = "",
    val distritoInmueble: String = "",
    val nombreUnidad: String = "",
    val numeroUnico: String = "",
    val idContrato: String = "",
    val fechaEmision: String = "",
    val rentaBase: Double = 0.0,
    val montoIgv: Double = 0.0,
    val agua: Double = 0.0,
    val luz: Double = 0.0,
    val mantenimiento: Double = 0.0,
    val montoTotal: Double = 0.0,
    val estadoPago: String = ESTADO_PENDIENTE
) {
    companion object {
        const val ESTADO_PENDIENTE = "Pendiente"
        const val ESTADO_COBRADO = "Cobrado"
    }
    // CRITERIO 6: Generar una lista solo con los conceptos válidos (> 0) para la UI
    fun obtenerConceptosCobrar(): List<Pair<String, Double>> {
        val conceptos = mutableListOf<Pair<String, Double>>()

        // Renta e IGV siempre van (asumiendo que rentaBase > 0)
        if (rentaBase > 0.0) conceptos.add(Pair("Renta", rentaBase))
        if (montoIgv > 0.0) conceptos.add(Pair("IGV (18%)", montoIgv))

        // Agregamos solo si el secretario ingresó un valor mayor a 0
        if (agua > 0.0) conceptos.add(Pair("Agua", agua))
        if (luz > 0.0) conceptos.add(Pair("Luz", luz))
        if (mantenimiento > 0.0) conceptos.add(Pair("mantenimiento", mantenimiento))

        return conceptos
    }
}
