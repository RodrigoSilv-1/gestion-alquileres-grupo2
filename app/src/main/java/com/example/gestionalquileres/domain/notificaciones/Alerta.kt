package com.example.gestionalquileres.domain.notificaciones

import com.example.gestionalquileres.domain.model.UserRole

/** Tipos de alerta y el rol al que le corresponde recibirla. */
enum class TipoAlerta(val destinatario: UserRole) {
    RECIBO_POR_VENCER(UserRole.SECRETARIO),
    RECIBO_VENCIDO(UserRole.SECRETARIO),
    EMITIR_RECIBOS(UserRole.SECRETARIO),
    CONTRATO_POR_VENCER(UserRole.ADMIN),
    CONTRATO_VENCIDO(UserRole.ADMIN)
}

/**
 * Una alerta lista para mostrar.
 * [referenciaId]: id del recibo o contrato (o "2026-11" para el recordatorio mensual).
 * [nivel]: etapa del aviso (30/15/7 días, o semana de retraso). Cambia cuando toca avisar otra vez.
 */
data class Alerta(
    val tipo: TipoAlerta,
    val referenciaId: String,
    val nivel: Int,
    val titulo: String,
    val mensaje: String
) {
    /** Identifica "este aviso concreto". En la Fase 3 sirve para no repetirlo. */
    val clave: String get() = "${tipo.name}|$referenciaId|$nivel"
}