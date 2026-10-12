package com.example.gestionalquileres.domain.model

data class Recibo(
    val idRecibo: String = "",
    val numeroUnico: String = "",
    val idContrato: String = "",
    val fechaEmision: String = "",
    val rentaBase: Double = 0.0,
    val montoIgv: Double = 0.0,
    val agua: Double = 0.0,
    val luz: Double = 0.0,
    val ipc: Double = 0.0,
    val montoTotal: Double = 0.0,
    val estadoPago: String = "Pendiente"
) {
    // CRITERIO 6: Generar una lista solo con los conceptos válidos (> 0) para la UI
    fun obtenerConceptosCobrar(): List<Pair<String, Double>> {
        val conceptos = mutableListOf<Pair<String, Double>>()

        // Renta e IGV siempre van (asumiendo que rentaBase > 0)
        if (rentaBase > 0.0) conceptos.add(Pair("Renta", rentaBase))
        if (montoIgv > 0.0) conceptos.add(Pair("IGV (18%)", montoIgv))

        // Agregamos solo si el secretario ingresó un valor mayor a 0
        if (agua > 0.0) conceptos.add(Pair("Agua", agua))
        if (luz > 0.0) conceptos.add(Pair("Luz", luz))
        if (ipc > 0.0) conceptos.add(Pair("IPC", ipc))

        return conceptos
    }
}