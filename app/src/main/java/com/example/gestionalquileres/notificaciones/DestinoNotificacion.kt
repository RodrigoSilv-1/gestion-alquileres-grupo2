package com.example.gestionalquileres.notificaciones

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Puente entre "tocaron una notificación" (MainActivity) y "navegar a la pantalla" (AppNavHost).
 * Guarda el destino hasta que haya sesión y la navegación pueda atenderlo.
 */
object DestinoNotificacion {

    const val EXTRA = "destino_notificacion"

    private val _pendiente = MutableStateFlow<String?>(null)
    val pendiente: StateFlow<String?> = _pendiente.asStateFlow()

    /** Lee el destino del intent (si trae uno) y lo quita, para no repetirlo al rotar la pantalla. */
    fun leerDe(intent: Intent?) {
        val destino = intent?.getStringExtra(EXTRA) ?: return
        intent.removeExtra(EXTRA)
        _pendiente.value = destino
    }

    fun consumir() {
        _pendiente.value = null
    }
}