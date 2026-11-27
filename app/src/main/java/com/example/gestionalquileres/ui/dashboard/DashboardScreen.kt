package com.example.gestionalquileres.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gestionalquileres.domain.comoMonto
import com.example.gestionalquileres.domain.model.CobranzaMes
import com.example.gestionalquileres.domain.model.Deudor
import com.example.gestionalquileres.domain.model.IngresoMensual
import com.example.gestionalquileres.domain.model.ResumenGeneral
import com.example.gestionalquileres.domain.sumarMontos
import com.example.gestionalquileres.ui.common.AppTopBar
import com.example.gestionalquileres.ui.theme.ExitoVerde
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                titulo = "Dashboard",
                onVolver = onVolver,
                actions = {
                    IconButton(onClick = viewModel::recargar) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SeccionResumen(uiState.resumen, uiState.cargandoResumen)
            SeccionCobranza(
                mes = uiState.mes,
                cobranza = uiState.cobranza,
                cargando = uiState.cargandoCobranza,
                puedeAvanzar = !uiState.esMesActual,
                onMesAnterior = { viewModel.cambiarMes(-1) },
                onMesSiguiente = { viewModel.cambiarMes(1) }
            )
            SeccionTendencia(uiState.ingresos, uiState.cargandoIngresos)
        }
    }
}

// ---------- Dashboard 1: Resumen general ----------

@Composable
private fun ColumnScope.SeccionResumen(resumen: ResumenGeneral?, cargando: Boolean) {
    Text("Resumen general", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

    if (resumen == null) {
        if (cargando) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        return
    }

    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TarjetaCifra(
            titulo = "Contratos vigentes",
            valor = "${resumen.contratosVigentes}",
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
        TarjetaCifra(
            titulo = "Cobrado este mes",
            valor = resumen.cobradoDelMes.comoMonto(),
            colorValor = ExitoVerde,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
    }
    TarjetaCifra(
        titulo = "Recibos pendientes",
        valor = "${resumen.recibosPendientes}",
        detalle = "Por cobrar: ${resumen.montoPendiente.comoMonto()}",
        colorValor = MaterialTheme.colorScheme.error,
        modifier = Modifier.fillMaxWidth()
    )

    // Gráfico: ocupación de unidades
    TarjetaDashboard {
        Text("Ocupación de unidades", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        if (resumen.unidadesTotales == 0) {
            Text("Aún no hay unidades registradas.", style = MaterialTheme.typography.bodyMedium)
        } else {
            val colorOcupadas = MaterialTheme.colorScheme.primary
            val colorDisponibles = MaterialTheme.colorScheme.outline
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                GraficoDona(
                    segmentos = listOf(
                        SegmentoDona("Ocupadas", resumen.unidadesOcupadas, colorOcupadas),
                        SegmentoDona("Disponibles", resumen.unidadesDisponibles, colorDisponibles)
                    ),
                    textoCentro = "${resumen.porcentajeOcupacion}%",
                    modifier = Modifier.size(150.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LeyendaGrafico(colorOcupadas, "Ocupadas: ${resumen.unidadesOcupadas}")
                    LeyendaGrafico(colorDisponibles, "Disponibles: ${resumen.unidadesDisponibles}")
                    Text("Total: ${resumen.unidadesTotales} unidades", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// ---------- Dashboard 2: Cobranza del mes ----------

@Composable
private fun ColumnScope.SeccionCobranza(
    mes: YearMonth,
    cobranza: CobranzaMes?,
    cargando: Boolean,
    puedeAvanzar: Boolean,
    onMesAnterior: () -> Unit,
    onMesSiguiente: () -> Unit
) {
    Text("Cobranza del mes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

    // Selector de mes
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onMesAnterior) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Mes anterior")
        }
        Text(mes.titulo(), style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onMesSiguiente, enabled = puedeAvanzar) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Mes siguiente")
        }
    }

    if (cobranza == null) {
        if (cargando) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        return
    }

    TarjetaDashboard {
        if (cobranza.totalRecibos == 0) {
            Text("No hay recibos emitidos en este mes.", style = MaterialTheme.typography.bodyMedium)
        } else {
            val colorPendiente = MaterialTheme.colorScheme.error

            // Gráfico: recibos pagados vs. pendientes
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                GraficoDona(
                    segmentos = listOf(
                        SegmentoDona("Pagados", cobranza.recibosPagados, ExitoVerde),
                        SegmentoDona("Pendientes", cobranza.recibosPendientes, colorPendiente)
                    ),
                    textoCentro = "${cobranza.porcentajeCobrado}%",
                    modifier = Modifier.size(150.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LeyendaGrafico(ExitoVerde, "Pagados: ${cobranza.recibosPagados}")
                    LeyendaGrafico(colorPendiente, "Pendientes: ${cobranza.recibosPendientes}")
                    Text("Total: ${cobranza.totalRecibos} recibos", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Gráfico: monto cobrado vs. por cobrar
            Text("Monto cobrado vs. por cobrar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            GraficoBarrasMonto(
                montoCobrado = cobranza.montoCobrado,
                montoPorCobrar = cobranza.montoPorCobrar,
                modifier = Modifier.fillMaxWidth().height(220.dp)
            )
        }

        HorizontalDivider()
        ListaDeudores(cobranza.mayoresDeudores)
    }
}

@Composable
private fun ListaDeudores(deudores: List<Deudor>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Mayores deudores (todos los meses)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        if (deudores.isEmpty()) {
            Text("No hay recibos pendientes.", style = MaterialTheme.typography.bodyMedium)
            return@Column
        }

        deudores.forEachIndexed { indice, deudor ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${indice + 1}. ${deudor.nombreInquilino}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    val unidad = if (deudor.nombreUnidad.isNotBlank()) "Unidad ${deudor.nombreUnidad} · " else ""
                    val plural = if (deudor.recibosPendientes == 1) "recibo" else "recibos"
                    Text("$unidad${deudor.recibosPendientes} $plural", style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    deudor.montoAdeudado.comoMonto(),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ---------- Dashboard 3: Tendencia de ingresos ----------

@Composable
private fun ColumnScope.SeccionTendencia(ingresos: List<IngresoMensual>, cargando: Boolean) {
    Text("Tendencia de ingresos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

    if (ingresos.isEmpty()) {
        if (cargando) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        return
    }

    TarjetaDashboard {
        val hayDatos = ingresos.any { it.montoCobrado > 0 || it.montoPorCobrar > 0 }
        if (!hayDatos) {
            Text("Aún no hay recibos en los últimos ${ingresos.size} meses.", style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(
                "Últimos ${ingresos.size} meses",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LeyendaGrafico(ExitoVerde, "Cobrado")
                LeyendaGrafico(MaterialTheme.colorScheme.error, "Por cobrar")
            }
            GraficoLineaIngresos(
                ingresos = ingresos,
                modifier = Modifier.fillMaxWidth().height(240.dp)
            )
            val total = sumarMontos(*ingresos.map { it.montoCobrado }.toDoubleArray())
            Text(
                "Total cobrado en el periodo: ${total.comoMonto()}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun YearMonth.titulo(): String =
    month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-PE"))
        .replaceFirstChar { it.uppercase() } + " $year"