package com.example.gestionalquileres.notificaciones

import com.example.gestionalquileres.domain.notificaciones.Alerta
import com.example.gestionalquileres.domain.notificaciones.TipoAlerta

object IdsNotificacion {

    const val RESUMEN_RECIBOS = 1001
    const val RESUMEN_CONTRATOS = 1002

    fun deRecibo(idRecibo: String): Int = "RECIBO|$idRecibo".hashCode()
    fun deContrato(idContrato: String): Int = "CONTRATO|$idContrato".hashCode()
    fun deEmision(mes: String): Int = "EMITIR|$mes".hashCode()

    /** "Por vencer" y "vencido" del mismo recibo comparten ID, así el segundo reemplaza al primero. */
    fun de(alerta: Alerta): Int = when (alerta.tipo) {
        TipoAlerta.RECIBO_POR_VENCER,
        TipoAlerta.RECIBO_VENCIDO -> deRecibo(alerta.referenciaId)

        TipoAlerta.CONTRATO_POR_VENCER,
        TipoAlerta.CONTRATO_VENCIDO -> deContrato(alerta.referenciaId)

        TipoAlerta.EMITIR_RECIBOS -> deEmision(alerta.referenciaId)
    }
}