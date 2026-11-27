package com.example.gestionalquileres.notificaciones

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject

/** Anota qué avisos ya se mostraron para no repetirlos. Cada anotación guarda el día en que se hizo. */
class RegistroAvisos @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("registro_avisos", Context.MODE_PRIVATE)

    fun yaAvisada(clave: String): Boolean = prefs.contains(clave)

    fun marcarAvisada(clave: String, hoy: LocalDate = LocalDate.now()) {
        prefs.edit().putLong(clave, hoy.toEpochDay()).apply()
    }

    /** Borra anotaciones viejas para que el registro no crezca sin límite. */
    fun limpiarAntiguas(hoy: LocalDate = LocalDate.now()) {
        val limite = hoy.toEpochDay() - DIAS_CONSERVAR
        val viejas = prefs.all.filterValues { (it as? Long ?: 0L) < limite }.keys
        if (viejas.isEmpty()) return
        prefs.edit().apply { viejas.forEach { remove(it) } }.apply()
    }

    /** Olvida todo: la próxima revisión volverá a avisar de todas las alertas vigentes. */
    fun limpiar() {
        prefs.edit().clear().apply()
    }

    private companion object {
        /** Ninguna etapa de aviso dura tanto, así que borrar después de 90 días no causa repeticiones. */
        const val DIAS_CONSERVAR = 90L
    }
}