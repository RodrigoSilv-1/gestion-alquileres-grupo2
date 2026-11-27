package com.example.gestionalquileres

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.fragment.app.FragmentActivity
import com.example.gestionalquileres.data.network.NetworkMonitor
import com.example.gestionalquileres.data.sync.SyncManager
import com.example.gestionalquileres.notificaciones.DestinoNotificacion
import com.example.gestionalquileres.ui.navigation.AppRoot
import com.example.gestionalquileres.ui.theme.GestionAlquileresTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    @Inject
    lateinit var syncManager: SyncManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Si la app se abrió desde una notificación, anota a qué pantalla ir.
        if (savedInstanceState == null) DestinoNotificacion.leerDe(intent)
        enableEdgeToEdge()

        setContent {
            GestionAlquileresTheme {
                LaunchedEffect(Unit) {
                    var estabaOnline = networkMonitor.isCurrentlyOnline()
                    networkMonitor.isOnline.collect { online ->
                        if (online && !estabaOnline) {
                            runCatching { syncManager.sincronizarTodo() }
                        }
                        estabaOnline = online
                    }
                }

                AppRoot()
            }
        }
    }

    /** La app ya estaba abierta y tocaron una notificación. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        DestinoNotificacion.leerDe(intent)
    }
}