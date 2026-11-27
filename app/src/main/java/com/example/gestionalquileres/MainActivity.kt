package com.example.gestionalquileres

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.fragment.app.FragmentActivity
import com.example.gestionalquileres.data.network.NetworkMonitor
import com.example.gestionalquileres.data.sync.SyncManager
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
}