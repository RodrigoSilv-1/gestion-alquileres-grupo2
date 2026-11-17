package com.example.gestionalquileres

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.gestionalquileres.ui.navigation.AppRoot
import com.example.gestionalquileres.ui.theme.GestionAlquileresTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GestionAlquileresTheme {
                AppRoot()
            }
        }
    }
}