package com.example.gestionalquileres.ui.notificaciones

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import com.example.gestionalquileres.notificaciones.ProgramadorAlertas
import java.time.Duration
import com.example.gestionalquileres.notificaciones.AlertasWorker
import com.example.gestionalquileres.notificaciones.PermisoNotificaciones
import com.example.gestionalquileres.notificaciones.RegistroAvisos
import java.util.UUID

private const val PREFS = "notificaciones_prefs"
private const val KEY_YA_PREGUNTADO = "permiso_ya_preguntado"

/**
 * Explica para qué sirven las notificaciones y pide el permiso (Android 13+).
 * Se pregunta una sola vez. Si el usuario dice "Ahora no", la app sigue funcionando igual.
 */
@Composable
fun SolicitarPermisoNotificaciones() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    var mostrarDialogo by remember {
        mutableStateOf(
            PermisoNotificaciones.requiereSolicitud(context) &&
                    !prefs.getBoolean(KEY_YA_PREGUNTADO, false)
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Sin acción: el chequeo se hace al momento de publicar cada notificación. */ }

    if (mostrarDialogo) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Activar notificaciones") },
            text = {
                Text("Te avisaremos de recibos por vencer o vencidos y de contratos próximos a terminar.")
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.edit().putBoolean(KEY_YA_PREGUNTADO, true).apply()
                    mostrarDialogo = false
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text("Activar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    prefs.edit().putBoolean(KEY_YA_PREGUNTADO, true).apply()
                    mostrarDialogo = false
                }) { Text("Ahora no") }
            }
        )
    }
}

/** TEMPORAL: solo aparece en builds de depuración. Ejecuta la revisión de alertas a mano. */
@Composable
fun BotonRevisarAlertas() {
    val context = LocalContext.current
    val esDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    if (esDebug) PanelAlertasDebug()
}

@Composable
private fun PanelAlertasDebug() {
    val context = LocalContext.current
    var resumen by remember { mutableStateOf<String?>(null) }
    var trabajoId by remember { mutableStateOf<UUID?>(null) }

    // Espera a que el Worker termine y muestra su resumen en pantalla.
    LaunchedEffect(trabajoId) {
        val id = trabajoId ?: return@LaunchedEffect
        WorkManager.getInstance(context).getWorkInfoByIdFlow(id).collect { info ->
            if (info != null && info.state.isFinished) {
                resumen = info.outputData.getString(AlertasWorker.CLAVE_RESUMEN)
                    ?: "Terminó con estado ${info.state}"
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                resumen = "Revisando…"
                val pedido = OneTimeWorkRequestBuilder<AlertasWorker>().build()
                trabajoId = pedido.id
                WorkManager.getInstance(context).enqueue(pedido)
            }) { Text("Revisar alertas") }

            Button(onClick = {
                RegistroAvisos(context.applicationContext).limpiar()
                NotificationManagerCompat.from(context).cancelAll()
                resumen = "Avisos reiniciados: se volverá a avisar de todo."
            }) { Text("Reiniciar avisos") }
        }

        // Prueba de la revisión diaria: la adelanta para que corra en ~1 minuto.
        Button(onClick = {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                ProgramadorAlertas.NOMBRE_DIARIO,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                ProgramadorAlertas.crearPedidoDiario(Duration.ofMinutes(1))
            )
            resumen = "Revisión diaria adelantada: correrá en 1 a 5 min aunque cierres la app. " +
                    "Para volver a la hora normal, cierra sesión y entra de nuevo."
        }) { Text("Programar en 1 min") }

        resumen?.let { texto ->
            Surface(
                modifier = Modifier.padding(top = 8.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color.White.copy(alpha = 0.90f)
            ) {
                Text(
                    text = texto,
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF222222)
                )
            }
        }
    }
}