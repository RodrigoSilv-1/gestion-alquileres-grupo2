package com.example.gestionalquileres.domain.model

/** Cifras de la pantalla de resumen. */
data class ResumenGeneral(
    val unidadesTotales: Int = 0,
    val contratosVigentes: Int = 0,
    val recibosPendientes: Int = 0,
    val montoPendiente: Double = 0.0,
    val cobradoDelMes: Double = 0.0
) {
    /** Cada contrato vigente ocupa una unidad. */
    val unidadesOcupadas: Int get() = contratosVigentes.coerceAtMost(unidadesTotales)
    val unidadesDisponibles: Int get() = unidadesTotales - unidadesOcupadas
    val porcentajeOcupacion: Int
        get() = if (unidadesTotales == 0) 0 else unidadesOcupadas * 100 / unidadesTotales
}

/** Inquilino (contrato) con recibos pendientes de pago. */
data class Deudor(
    val idContrato: String,
    val nombreInquilino: String,
    val nombreUnidad: String,
    val recibosPendientes: Int,
    val montoAdeudado: Double
)

/** Cobranza de un mes + los mayores deudores (estos últimos, de todos los meses). */
data class CobranzaMes(
    val recibosPagados: Int = 0,
    val recibosPendientes: Int = 0,
    val montoCobrado: Double = 0.0,
    val montoPorCobrar: Double = 0.0,
    val mayoresDeudores: List<Deudor> = emptyList()
) {
    val totalRecibos: Int get() = recibosPagados + recibosPendientes
    val porcentajeCobrado: Int get() = if (totalRecibos == 0) 0 else recibosPagados * 100 / totalRecibos
}