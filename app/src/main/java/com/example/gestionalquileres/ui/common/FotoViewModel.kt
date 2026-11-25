package com.example.gestionalquileres.ui.common

import androidx.lifecycle.ViewModel
import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.FotoRepository
import com.example.gestionalquileres.domain.model.TipoFoto
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FotoViewModel @Inject constructor(
    private val repository: FotoRepository
) : ViewModel() {

    /** Foto grande en Base64, o null si no existe (registros antiguos) o falló la lectura. */
    suspend fun obtener(tipo: TipoFoto, idPropietario: String): String? =
        when (val resultado = repository.obtener(tipo, idPropietario)) {
            is AppResult.Exito -> resultado.valor
            is AppResult.Error -> null
        }
}