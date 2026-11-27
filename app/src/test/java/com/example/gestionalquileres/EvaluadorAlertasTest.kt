package com.example.gestionalquileres.domain.notificaciones

import com.example.gestionalquileres.domain.model.Contrato
import com.example.gestionalquileres.domain.model.Recibo
import com.example.gestionalquileres.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class EvaluadorAlertasTest {

    private val evaluador = EvaluadorAlertas()
    private val hoy = LocalDate.of(2026, 11, 26)

    private fun recibo(
        id: String,
        emision: String,
        estado: String = "Pendiente",
        idContrato: String = "c1"
    ) = Recibo(
        idRecibo = id,
        nombreInquilino = "Ana Pérez",
        nombreUnidad = "Depto. 201",
        numeroUnico = "REC-$id",
        idContrato = idContrato,
        fechaEmision = emision,
        montoTotal = 1500.0,
        estadoPago = estado
    )

    private fun contrato(
        id: String,
        fin: String,
        inicio: String = "2026-01-01",
        active: Boolean = true
    ) = Contrato(
        idContrato = id,
        nombreInquilino = "Ana Pérez",
        nombreUnidad = "Depto. 201",
        fechaInicio = inicio,
        fechaFin = fin,
        active = active
    )

    private fun alertasSecretario(recibos: List<Recibo>, contratos: List<Contrato> = emptyList()) =
        evaluador.evaluar(UserRole.SECRETARIO, recibos, contratos, hoy)

    private fun alertasAdmin(contratos: List<Contrato>, recibos: List<Recibo> = emptyList()) =
        evaluador.evaluar(UserRole.ADMIN, recibos, contratos, hoy)

    // ---------- Recibos ----------

    @Test
    fun `recibo recien emitido no genera alerta`() {
        // emitido hoy: vence en 5 días, fuera del aviso previo de 2
        assertTrue(alertasSecretario(listOf(recibo("1", "2026-11-26"))).isEmpty())
    }

    @Test
    fun `recibo que vence en 2 dias genera aviso por vencer`() {
        val alertas = alertasSecretario(listOf(recibo("1", "2026-11-23")))
        assertEquals(1, alertas.size)
        assertEquals(TipoAlerta.RECIBO_POR_VENCER, alertas[0].tipo)
    }

    @Test
    fun `recibo que vence hoy dice que vence hoy`() {
        val alertas = alertasSecretario(listOf(recibo("1", "2026-11-21")))
        assertEquals(TipoAlerta.RECIBO_POR_VENCER, alertas[0].tipo)
        assertEquals("Recibo vence hoy", alertas[0].titulo)
    }

    @Test
    fun `recibo vencido ayer genera alerta de vencido nivel 0`() {
        val alertas = alertasSecretario(listOf(recibo("1", "2026-11-20")))
        assertEquals(TipoAlerta.RECIBO_VENCIDO, alertas[0].tipo)
        assertEquals(0, alertas[0].nivel)
    }

    @Test
    fun `recibo vencido hace 7 dias sigue en nivel 0 y hace 8 dias pasa a nivel 1`() {
        val hace7 = alertasSecretario(listOf(recibo("1", "2026-11-14")))
        val hace8 = alertasSecretario(listOf(recibo("2", "2026-11-13")))
        assertEquals(0, hace7[0].nivel)
        assertEquals(1, hace8[0].nivel)
    }

    @Test
    fun `recibo pagado no genera alerta`() {
        assertTrue(alertasSecretario(listOf(recibo("1", "2026-11-01", estado = "Pagado"))).isEmpty())
    }

    @Test
    fun `recibo con estado antiguo Cobrado se trata como pagado`() {
        assertTrue(alertasSecretario(listOf(recibo("1", "2026-11-01", estado = "Cobrado"))).isEmpty())
    }

    @Test
    fun `fecha invalida no rompe y se ignora`() {
        assertTrue(alertasSecretario(listOf(recibo("1", "no-es-fecha"), recibo("2", ""))).isEmpty())
    }

    @Test
    fun `fecha con hora se interpreta bien`() {
        val alertas = alertasSecretario(listOf(recibo("1", "2026-11-20T10:30:00")))
        assertEquals(TipoAlerta.RECIBO_VENCIDO, alertas[0].tipo)
    }

    // ---------- Contratos ----------

    @Test
    fun `contrato a 30 dias genera aviso nivel 30`() {
        val alertas = alertasAdmin(listOf(contrato("k1", "2026-12-26")))
        assertEquals(TipoAlerta.CONTRATO_POR_VENCER, alertas[0].tipo)
        assertEquals(30, alertas[0].nivel)
    }

    @Test
    fun `contrato a 31 dias todavia no genera aviso`() {
        assertTrue(alertasAdmin(listOf(contrato("k1", "2026-12-27"))).isEmpty())
    }

    @Test
    fun `contrato a 14 dias genera nivel 15`() {
        assertEquals(15, alertasAdmin(listOf(contrato("k1", "2026-12-10")))[0].nivel)
    }

    @Test
    fun `contrato a 6 dias genera nivel 7`() {
        assertEquals(7, alertasAdmin(listOf(contrato("k1", "2026-12-02")))[0].nivel)
    }

    @Test
    fun `contrato que termina hoy es aviso por vencer`() {
        val alerta = alertasAdmin(listOf(contrato("k1", "2026-11-26")))[0]
        assertEquals(TipoAlerta.CONTRATO_POR_VENCER, alerta.tipo)
    }

    @Test
    fun `contrato vigente con fecha pasada genera alerta de vencido`() {
        val alerta = alertasAdmin(listOf(contrato("k1", "2026-11-25")))[0]
        assertEquals(TipoAlerta.CONTRATO_VENCIDO, alerta.tipo)
        assertEquals(0, alerta.nivel)
    }

    @Test
    fun `contrato inactivo no genera alerta`() {
        assertTrue(alertasAdmin(listOf(contrato("k1", "2026-12-01", active = false))).isEmpty())
    }

    // ---------- Roles ----------

    @Test
    fun `el secretario no recibe alertas de contratos`() {
        val alertas = alertasSecretario(emptyList(), listOf(contrato("k1", "2026-12-01")))
        assertTrue(alertas.none { it.tipo.destinatario != UserRole.SECRETARIO })
        assertTrue(alertas.none { it.tipo == TipoAlerta.CONTRATO_POR_VENCER })
    }

    @Test
    fun `el administrador no recibe alertas de recibos`() {
        val alertas = alertasAdmin(emptyList(), listOf(recibo("1", "2026-11-01")))
        assertTrue(alertas.isEmpty())
    }

    // ---------- Recordatorio de emisión ----------

    @Test
    fun `contrato vigente sin recibo este mes genera una sola alerta agrupada`() {
        val contratos = listOf(contrato("k1", "2027-06-01"), contrato("k2", "2027-06-01"))
        val alertas = alertasSecretario(emptyList(), contratos)
        assertEquals(1, alertas.size)
        assertEquals(TipoAlerta.EMITIR_RECIBOS, alertas[0].tipo)
        assertEquals("2026-11", alertas[0].referenciaId)
    }

    @Test
    fun `contrato con recibo emitido este mes no genera recordatorio`() {
        val contratos = listOf(contrato("k1", "2027-06-01"))
        val recibos = listOf(recibo("1", "2026-11-03", estado = "Pagado", idContrato = "k1"))
        assertTrue(alertasSecretario(recibos, contratos).isEmpty())
    }

    @Test
    fun `recibo de un mes anterior no cuenta como emitido este mes`() {
        val contratos = listOf(contrato("k1", "2027-06-01"))
        val recibos = listOf(recibo("1", "2026-10-03", estado = "Pagado", idContrato = "k1"))
        val alertas = alertasSecretario(recibos, contratos)
        assertEquals(TipoAlerta.EMITIR_RECIBOS, alertas[0].tipo)
    }

    @Test
    fun `contrato que aun no empieza no genera recordatorio`() {
        val contratos = listOf(contrato("k1", "2027-12-01", inicio = "2026-12-01"))
        assertTrue(alertasSecretario(emptyList(), contratos).isEmpty())
    }

    // ---------- Clave para evitar duplicados (Fase 3) ----------

    @Test
    fun `la clave cambia cuando cambia el nivel`() {
        val a = alertasAdmin(listOf(contrato("k1", "2026-12-26")))[0]   // nivel 30
        val b = alertasAdmin(listOf(contrato("k1", "2026-12-10")))[0]   // nivel 15
        assertTrue(a.clave != b.clave)
    }
}