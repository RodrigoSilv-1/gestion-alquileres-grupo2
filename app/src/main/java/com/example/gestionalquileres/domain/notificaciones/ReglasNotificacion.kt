package com.example.gestionalquileres.domain.notificaciones

/** Reglas de las alertas. Cambia un valor aquí y cambia en todo el sistema. */
object ReglasNotificacion {

    /** Días después de la fecha de emisión en que un recibo pendiente se considera vencido. */
    const val DIAS_GRACIA_RECIBO = 5

    /** Cuántos días antes del vencimiento se avisa que un recibo está por vencer (0 = el mismo día). */
    const val DIAS_AVISO_PREVIO_RECIBO = 2

    /** Cada cuántos días se repite el aviso de algo que sigue vencido. */
    const val DIAS_REPETIR_VENCIDO = 7

    /** Días de anticipación para avisar el fin de un contrato. */
    val DIAS_AVISO_CONTRATO = listOf(30, 15, 7)

    /** Desde qué día del mes se avisa de contratos sin recibo emitido. */
    const val DIA_RECORDATORIO_EMISION = 1
}