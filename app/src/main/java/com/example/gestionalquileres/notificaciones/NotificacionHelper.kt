package com.example.gestionalquileres.notificaciones

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.gestionalquileres.MainActivity
import com.example.gestionalquileres.R
import com.example.gestionalquileres.domain.notificaciones.ReglasNotificacion
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificacionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** Publica una notificación. [destino] es la ruta a abrir al tocarla. Devuelve false si no se pudo. */
    @SuppressLint("MissingPermission") // el permiso se verifica en hayPermiso()
    fun mostrar(
        canal: CanalNotificacion,
        idNotificacion: Int,
        titulo: String,
        mensaje: String,
        destino: String? = null
    ): Boolean {
        if (!hayPermiso(titulo)) return false

        val notificacion = NotificationCompat.Builder(context, canal.id)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setContentIntent(intentAbrirApp(idNotificacion, destino))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(idNotificacion, notificacion)
        return true
    }

    /** Publica una sola notificación que lista varios avisos (se expande al deslizar). */
    @SuppressLint("MissingPermission") // el permiso se verifica en hayPermiso()
    fun mostrarResumen(
        canal: CanalNotificacion,
        idNotificacion: Int,
        titulo: String,
        lineas: List<String>,
        destino: String? = null
    ): Boolean {
        if (!hayPermiso(titulo)) return false

        val estilo = NotificationCompat.InboxStyle().setBigContentTitle(titulo)
        lineas.take(ReglasNotificacion.MAX_LINEAS_RESUMEN).forEach { estilo.addLine(it) }
        val restantes = lineas.size - ReglasNotificacion.MAX_LINEAS_RESUMEN
        if (restantes > 0) estilo.setSummaryText("y $restantes más")

        val notificacion = NotificationCompat.Builder(context, canal.id)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText("Toca para ver el detalle")
            .setStyle(estilo)
            .setContentIntent(intentAbrirApp(idNotificacion, destino))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(idNotificacion, notificacion)
        return true
    }

    fun cancelar(idNotificacion: Int) {
        NotificationManagerCompat.from(context).cancel(idNotificacion)
    }

    private fun hayPermiso(titulo: String): Boolean {
        val concedido = PermisoNotificaciones.concedido(context)
        if (!concedido) Log.w(TAG, "No se publicó '$titulo': las notificaciones no están permitidas")
        return concedido
    }

    private fun intentAbrirApp(idNotificacion: Int, destino: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            // SINGLE_TOP: si la app ya está abierta, la reutiliza (onNewIntent) en vez de reiniciarla.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            destino?.let { putExtra(DestinoNotificacion.EXTRA, it) }
        }
        return PendingIntent.getActivity(
            context,
            idNotificacion, // distinto por notificación, para que cada una conserve su propio destino
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private companion object {
        const val TAG = "Notificaciones"
    }
}