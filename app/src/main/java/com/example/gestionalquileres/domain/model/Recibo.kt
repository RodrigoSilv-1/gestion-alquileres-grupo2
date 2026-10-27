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
    // Bloque estático para definir constantes globales reutilizables relacionadas con los estados del recibo
    companion object {
        const val ESTADO_PENDIENTE = "Pendiente" // Constante que representa el estado pendiente de pago
        const val ESTADO_COBRADO = "Cobrado"     // Constante que representa el estado pagado/cobrado
    }

    // CRITERIO 6: Función que genera una lista estructurada con los conceptos válidos cuyo valor sea mayor a 0 para mostrarlos en la UI
    fun obtenerConceptosCobrar(): List<Pair<String, Double>> {
        // Creamos una lista mutable para ir almacenando dinámicamente los conceptos que cumplan la condición
        val conceptos = mutableListOf<Pair<String, Double>>()

        // Verificamos y agregamos la Renta y el IGV si su valor es mayor a 0 (conceptos base obligatorios si aplican)
        if (rentaBase > 0.0) conceptos.add(Pair("Renta", rentaBase))
        if (montoIgv > 0.0) conceptos.add(Pair("IGV (18%)", montoIgv))

        // Agregamos servicios adicionales (Agua, Luz y Mantenimiento) solo si se ingresó un monto mayor a 0
        if (agua > 0.0) conceptos.add(Pair("Agua", agua))
        if (luz > 0.0) conceptos.add(Pair("Luz", luz))
        if (mantenimiento > 0.0) conceptos.add(Pair("mantenimiento", mantenimiento))

        // Retornamos la lista final limpia con los conceptos listos para ser presentados en la interfaz de usuario
        return conceptos
    }
}
