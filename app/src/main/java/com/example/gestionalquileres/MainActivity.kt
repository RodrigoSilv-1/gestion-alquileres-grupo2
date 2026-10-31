package com.example.gestionalquileres

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.gestionalquileres.ui.auth.AuthViewModel
import com.example.gestionalquileres.ui.contrato.ContratoViewModel
import com.example.gestionalquileres.ui.inmuebles.InmuebleViewModel
import com.example.gestionalquileres.ui.inquilinos.InquilinoViewModel
import com.example.gestionalquileres.ui.navigation.AppRoot
import com.example.gestionalquileres.ui.recibos.ReciboViewModel
import com.example.gestionalquileres.ui.theme.GestionAlquileresTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val inquilinoViewModel: InquilinoViewModel by viewModels()
    private val inmuebleViewModel: InmuebleViewModel by viewModels()
    private val contratoViewModel: ContratoViewModel by viewModels()
    private val reciboViewModel: ReciboViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GestionAlquileresTheme {
                AppRoot(
                    authViewModel = authViewModel,
                    inquilinoViewModel = inquilinoViewModel,
                    inmuebleViewModel = inmuebleViewModel,
                    contratoViewModel = contratoViewModel,
                    reciboViewModel = reciboViewModel
                )
            }
        }
    }
}
