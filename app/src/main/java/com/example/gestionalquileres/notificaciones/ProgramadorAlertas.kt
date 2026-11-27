package com.example.gestionalquileres.notificaciones

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.gestionalquileres.domain.notificaciones.ReglasNotificacion.HORA_ENVIO
import com.example.gestionalquileres.domain.notificaciones.ReglasNotificacion.MINUTO_ENVIO
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Enciende y apaga el sistema de alertas según haya o no sesión abierta. */
@Singleton
class ProgramadorAlertas @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sesionAlertas: SesionAlertas,
    private val registroAvisos: RegistroAvisos
) {
    private val workManager get() = WorkManager.getInstance(context)

    /**
     * Llamar cuando hay sesión. Es seguro llamarlo en cada arranque de la app:
     * - Deja programada la revisión diaria (KEEP: si ya existe, no la reinicia).
     * - Si hoy ya pasó la hora de envío y aún no se revisó, hace una revisión de alcance ahora.
     */
    fun activar() {
        workManager.enqueueUniquePeriodicWork(
            NOMBRE_DIARIO,
            ExistingPeriodicWorkPolicy.KEEP,
            crearPedidoDiario(retrasoHastaHoraEnvio())
        )

        if (correspondeRevisionDeAlcance()) {
            workManager.enqueueUniqueWork(
                NOMBRE_ALCANCE,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<AlertasWorker>().build()
            )
        }
    }

    /** Llamar al cerrar sesión (o cuando ya no hay sesión): apaga todo y borra el rastro. */
    fun desactivar() {
        workManager.cancelUniqueWork(NOMBRE_DIARIO)
        workManager.cancelUniqueWork(NOMBRE_ALCANCE)
        sesionAlertas.limpiar()
        registroAvisos.limpiar()
        NotificationManagerCompat.from(context).cancelAll()
    }

    private fun correspondeRevisionDeAlcance(): Boolean {
        val ahora = LocalDateTime.now()
        val yaPasoLaHora = !ahora.toLocalTime().isBefore(LocalTime.of(HORA_ENVIO, MINUTO_ENVIO))
        val hoy = ahora.toLocalDate().toEpochDay()
        return yaPasoLaHora && sesionAlertas.ultimaRevision() != hoy
    }

    companion object {
        /** Si algún día se cambia la hora o la lógica de programación, se sube la versión (_v2) para que se reprograme. */
        const val NOMBRE_DIARIO = "alertas_diarias_v1"
        const val NOMBRE_ALCANCE = "alertas_alcance"

        /** Tiempo que falta hasta la próxima hora de envío (hoy si aún no pasó, mañana si ya pasó). */
        fun retrasoHastaHoraEnvio(): Duration {
            val ahora = LocalDateTime.now()
            var proxima = ahora.toLocalDate().atTime(HORA_ENVIO, MINUTO_ENVIO)
            if (!proxima.isAfter(ahora)) proxima = proxima.plusDays(1)
            return Duration.between(ahora, proxima)
        }

        fun crearPedidoDiario(retrasoInicial: Duration): PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<AlertasWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(retrasoInicial.toMillis(), TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
    }
}