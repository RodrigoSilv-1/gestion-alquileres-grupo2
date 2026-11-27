package com.example.gestionalquileres.notificaciones

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object PermisoNotificaciones {

    /** True si hay que pedir el permiso en pantalla (solo Android 13+ y aún no concedido). */
    fun requiereSolicitud(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED

    /** True si hoy se pueden publicar notificaciones (permiso concedido y no bloqueadas en ajustes). */
    fun concedido(context: Context): Boolean =
        !requiereSolicitud(context) &&
                NotificationManagerCompat.from(context).areNotificationsEnabled()
}