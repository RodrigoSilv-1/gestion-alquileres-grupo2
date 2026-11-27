package com.example.gestionalquileres.notificaciones

import android.content.Context
import com.example.gestionalquileres.domain.model.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Guarda quién tiene la sesión abierta, con qué rol y cuándo se revisaron las alertas por última vez. */
class SesionAlertas @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("sesion_alertas", Context.MODE_PRIVATE)

    fun guardar(uid: String, rol: UserRole) {
        val cambioDeUsuario = prefs.getString(KEY_UID, null) != uid
        prefs.edit().apply {
            if (cambioDeUsuario) remove(KEY_REVISION) // otro usuario: que se revise de nuevo hoy
            putString(KEY_UID, uid)
            putString(KEY_ROL, rol.name)
        }.apply()
    }

    /** El rol guardado, solo si pertenece al usuario [uid]. Si es de otro usuario, devuelve null. */
    fun rolDe(uid: String): UserRole? {
        if (prefs.getString(KEY_UID, null) != uid) return null
        val nombre = prefs.getString(KEY_ROL, null) ?: return null
        return UserRole.values().firstOrNull { it.name == nombre }
    }

    /** Día (número de día desde 1970) de la última revisión completada, o -1 si nunca. */
    fun ultimaRevision(): Long = prefs.getLong(KEY_REVISION, -1L)

    fun marcarRevision(epochDay: Long) {
        prefs.edit().putLong(KEY_REVISION, epochDay).apply()
    }

    fun limpiar() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_UID = "uid"
        const val KEY_ROL = "rol"
        const val KEY_REVISION = "ultima_revision"
    }
}