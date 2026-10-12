package com.example.gestionalquileres.data.auth.repository

import com.example.gestionalquileres.domain.model.AppResult
import kotlin.coroutines.cancellation.CancellationException

/** Ejecuta [bloque] y lo envuelve en AppResult. Relanza la cancelación de corrutinas. */
internal inline fun <T> appResultOf(bloque: () -> T): AppResult<T> =
    try {
        AppResult.Exito(bloque())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Error(e)
    }