package com.example.gestionalquileres.ui.notificaciones

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.gestionalquileres.notificaciones.PermisoNotificaciones

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