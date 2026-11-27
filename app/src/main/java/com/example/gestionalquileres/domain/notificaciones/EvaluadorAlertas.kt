package com.example.gestionalquileres.domain.notificaciones

import com.example.gestionalquileres.domain.comoMonto
import com.example.gestionalquileres.domain.model.Contrato
import com.example.gestionalquileres.domain.model.EstadoPago
import com.example.gestionalquileres.domain.model.Recibo
import com.example.gestionalquileres.domain.model.UserRole
import com.example.gestionalquileres.domain.notificaciones.ReglasNotificacion.DIAS_AVISO_CONTRATO
import com.example.gestionalquileres.domain.notificaciones.ReglasNotificacion.DIAS_AVISO_PREVIO_RECIBO
import com.example.gestionalquileres.domain.notificaciones.ReglasNotificacion.DIAS_GRACIA_RECIBO
import com.example.gestionalquileres.domain.notificaciones.ReglasNotificacion.DIAS_REPETIR_VENCIDO
import com.example.gestionalquileres.domain.notificaciones.ReglasNotificacion.DIA_RECORDATORIO_EMISION
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

/**
 * Decide qué alertas corresponden hoy. Sin Android ni red: recibe los datos y la fecha ([hoy]),
 * así se puede probar con fechas fijas.
 */
class EvaluadorAlertas @Inject constructor() {

    fun evaluar(
        rol: UserRole,
        recibos: List<Recibo>,
        contratos: List<Contrato>,
        hoy: LocalDate
    ): List<Alerta> = when (rol) {
        UserRole.SECRETARIO -> alertasDeRecibos(recibos, hoy) + alertasDeEmision(recibos, contratos, hoy)
        UserRole.ADMIN -> alertasDeContratos(contratos, hoy)
    }

    // ---------- Recibos pendientes (Secretario) ----------

    private fun alertasDeRecibos(recibos: List<Recibo>, hoy: LocalDate): List<Alerta> =
        recibos
            .filter { it.estado() == EstadoPago.PENDIENTE }
            .mapNotNull { recibo ->
                val emision = recibo.fechaEmision.aFecha() ?: return@mapNotNull null
                val vencimiento = emision.plusDays(DIAS_GRACIA_RECIBO.toLong())
                val diasParaVencer = ChronoUnit.DAYS.between(hoy, vencimiento)
                val referencia = recibo.idRecibo.ifBlank { recibo.numeroUnico }
                val detalle = "${recibo.nombreInquilino} · ${recibo.nombreUnidad}: " +
                        "${recibo.numeroUnico} (${recibo.montoTotal.comoMonto()})"

                when {
                    diasParaVencer < 0 -> {
                        val diasVencido = -diasParaVencer
                        Alerta(
                            tipo = TipoAlerta.RECIBO_VENCIDO,
                            referenciaId = referencia,
                            nivel = nivelDeRepeticion(diasVencido),
                            titulo = "Recibo vencido",
                            mensaje = "$detalle venció hace ${diasTexto(diasVencido)}."
                        )
                    }
                    diasParaVencer <= DIAS_AVISO_PREVIO_RECIBO -> Alerta(
                        tipo = TipoAlerta.RECIBO_POR_VENCER,
                        referenciaId = referencia,
                        nivel = 0,
                        titulo = if (diasParaVencer == 0L) "Recibo vence hoy" else "Recibo por vencer",
                        mensaje = if (diasParaVencer == 0L) "$detalle vence hoy."
                        else "$detalle vence en ${diasTexto(diasParaVencer)}."
                    )
                    else -> null
                }
            }

    // ---------- Recibos del mes sin emitir (Secretario) ----------

    private fun alertasDeEmision(
        recibos: List<Recibo>,
        contratos: List<Contrato>,
        hoy: LocalDate
    ): List<Alerta> {
        if (hoy.dayOfMonth < DIA_RECORDATORIO_EMISION) return emptyList()

        val mes = YearMonth.from(hoy)
        val contratosConReciboEsteMes = recibos
            .filter { recibo -> recibo.fechaEmision.aFecha()?.let { YearMonth.from(it) == mes } == true }
            .map { it.idContrato }
            .toSet()

        val sinRecibo = contratos.filter { contrato ->
            val inicio = contrato.fechaInicio.aFecha()
            val fin = contrato.fechaFin.aFecha()
            contrato.active &&
                    inicio != null && !inicio.isAfter(hoy) &&
                    (fin == null || !fin.isBefore(hoy)) &&
                    contrato.idContrato !in contratosConReciboEsteMes
        }
        if (sinRecibo.isEmpty()) return emptyList()

        val nombreMes = mes.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-PE"))
        val cantidad = sinRecibo.size
        return listOf(
            Alerta(
                tipo = TipoAlerta.EMITIR_RECIBOS,
                referenciaId = mes.toString(),          // "2026-11": un aviso por mes
                nivel = 0,
                titulo = "Faltan recibos de $nombreMes ${mes.year}",
                mensaje = if (cantidad == 1) "1 contrato vigente aún no tiene recibo emitido este mes."
                else "$cantidad contratos vigentes aún no tienen recibo emitido este mes."
            )
        )
    }

    // ---------- Contratos (Administrador) ----------

    private fun alertasDeContratos(contratos: List<Contrato>, hoy: LocalDate): List<Alerta> =
        contratos
            .filter { it.active }
            .mapNotNull { contrato ->
                val fin = contrato.fechaFin.aFecha() ?: return@mapNotNull null
                val diasRestantes = ChronoUnit.DAYS.between(hoy, fin)
                val detalle = "${contrato.nombreInquilino} · ${contrato.nombreUnidad}"

                if (diasRestantes < 0) {
                    val diasVencido = -diasRestantes
                    Alerta(
                        tipo = TipoAlerta.CONTRATO_VENCIDO,
                        referenciaId = contrato.idContrato,
                        nivel = nivelDeRepeticion(diasVencido),
                        titulo = "Contrato vencido",
                        mensaje = "$detalle: terminó hace ${diasTexto(diasVencido)} y sigue vigente."
                    )
                } else {
                    // Etapa de aviso: el menor umbral que todavía cubre los días restantes.
                    val umbral = DIAS_AVISO_CONTRATO.filter { diasRestantes <= it }.minOrNull()
                        ?: return@mapNotNull null
                    Alerta(
                        tipo = TipoAlerta.CONTRATO_POR_VENCER,
                        referenciaId = contrato.idContrato,
                        nivel = umbral,
                        titulo = "Contrato por vencer",
                        mensaje = if (diasRestantes == 0L) "$detalle: el contrato termina hoy."
                        else "$detalle: el contrato termina en ${diasTexto(diasRestantes)}."
                    )
                }
            }

    // ---------- Utilidades ----------

    /** Semana de retraso: días 1-7 → 0, días 8-14 → 1, etc. Al cambiar el nivel se vuelve a avisar. */
    private fun nivelDeRepeticion(diasVencido: Long): Int =
        ((diasVencido - 1) / DIAS_REPETIR_VENCIDO).toInt()

    private fun diasTexto(dias: Long): String = if (dias == 1L) "1 día" else "$dias días"

    /** Las fechas de la app son "yyyy-MM-dd" (a veces con hora detrás). Si no se entienden, se ignoran. */
    private fun String.aFecha(): LocalDate? =
        runCatching { LocalDate.parse(trim().take(10)) }.getOrNull()
}