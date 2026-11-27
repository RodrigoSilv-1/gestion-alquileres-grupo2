package com.example.gestionalquileres.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gestionalquileres.ui.dashboard.DashboardScreen
import com.example.gestionalquileres.domain.model.AppUser
import com.example.gestionalquileres.domain.model.UserRole
import com.example.gestionalquileres.ui.contrato.ContratoScreen
import com.example.gestionalquileres.ui.home.HomeScreen
import com.example.gestionalquileres.ui.inmuebles.InmuebleScreen
import com.example.gestionalquileres.ui.inquilinos.InquilinoScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.gestionalquileres.notificaciones.DestinoNotificacion
import com.example.gestionalquileres.ui.recibos.ReciboScreen

/**
 * Grafo de navegación de la parte autenticada.
 * Cada pantalla obtiene su ViewModel con hiltViewModel(), ligado a su entrada del back stack:
 * se crea al entrar y se destruye al salir.
 */
@Composable
fun AppNavHost(
    user: AppUser,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val esAdmin = user.role == UserRole.ADMIN.name

    // Si el usuario tocó una notificación, lo lleva a la pantalla que corresponde,
    // pero solo si su rol tiene esa pantalla.
    val destinoPendiente by DestinoNotificacion.pendiente.collectAsState()
    LaunchedEffect(destinoPendiente) {
        val destino = destinoPendiente ?: return@LaunchedEffect
        val permitidas = if (esAdmin) {
            setOf(Rutas.INQUILINOS, Rutas.INMUEBLES, Rutas.CONTRATOS)
        } else {
            setOf(Rutas.RECIBOS, Rutas.DASHBOARD)
        }
        if (destino in permitidas) navController.irA(destino)
        DestinoNotificacion.consumir()
    }

    NavHost(navController = navController, startDestination = Rutas.HOME) {

        composable(Rutas.HOME) {
            HomeScreen(
                user = user,
                onLogout = onLogout,
                onIrInquilinos = { navController.irA(Rutas.INQUILINOS) },
                onIrInmuebles = { navController.irA(Rutas.INMUEBLES) },
                onIrContratos = { navController.irA(Rutas.CONTRATOS) },
                onIrRecibos = { navController.irA(Rutas.RECIBOS) },
                onIrDashboard = { navController.irA(Rutas.DASHBOARD) }
            )
        }

        // Las rutas se registran según el rol: el Secretario no puede llegar a las del Administrador
        if (esAdmin) {
            composable(Rutas.INQUILINOS) {
                InquilinoScreen(viewModel = hiltViewModel(), onVolver = { navController.volver() })
            }
            composable(Rutas.INMUEBLES) {
                InmuebleScreen(viewModel = hiltViewModel(), onVolver = { navController.volver() })
            }
            composable(Rutas.CONTRATOS) {
                ContratoScreen(viewModel = hiltViewModel(), onVolver = { navController.volver() })
            }
        } else {
            composable(Rutas.RECIBOS) {
                ReciboScreen(viewModel = hiltViewModel(), onVolver = { navController.volver() })
            }
            composable(Rutas.DASHBOARD) {
                DashboardScreen(viewModel = hiltViewModel(), onVolver = { navController.volver() })
            }
        }
    }
}

/** Evita duplicar la pantalla si el usuario toca el botón dos veces. */
private fun NavHostController.irA(ruta: String) =
    navigate(ruta) { launchSingleTop = true }

/** Evita vaciar el back stack si "volver" se dispara dos veces seguidas. */
private fun NavHostController.volver() {
    if (previousBackStackEntry != null) popBackStack()
}