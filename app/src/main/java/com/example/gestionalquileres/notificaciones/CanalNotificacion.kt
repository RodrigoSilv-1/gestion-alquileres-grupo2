package com.example.gestionalquileres.notificaciones

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/** Cada canal aparece por separado en los ajustes del teléfono para que el usuario pueda silenciarlo. */
enum class CanalNotificacion(
    val id: String,
    val nombre: String,
    val descripcion: String
) {
    RECIBOS("canal_recibos", "Recibos", "Recibos por vencer, vencidos y por emitir"),
    CONTRATOS("canal_contratos", "Contratos", "Contratos próximos a vencer o vencidos");

    companion object {
        /** Crea todos los canales. Es seguro llamarlo siempre: Android ignora los que ya existen. */
        fun crearTodos(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java)
            val canales = values().map { canal ->
                NotificationChannel(canal.id, canal.nombre, NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = canal.descripcion }
            }
            manager.createNotificationChannels(canales)
        }
    }
}