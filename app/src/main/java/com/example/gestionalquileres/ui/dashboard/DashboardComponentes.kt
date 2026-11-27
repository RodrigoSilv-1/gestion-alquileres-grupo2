package com.example.gestionalquileres.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import com.example.gestionalquileres.domain.model.IngresoMensual
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.time.format.TextStyle
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.gestionalquileres.domain.comoMonto
import com.example.gestionalquileres.ui.theme.ExitoVerde
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import java.util.Locale

/** Una porción de la dona. */
data class SegmentoDona(val etiqueta: String, val valor: Int, val color: Color)

/** Tarjeta con una cifra grande (mismo estilo que las demás tarjetas de la app). */
@Composable
fun TarjetaCifra(
    titulo: String,
    valor: String,
    modifier: Modifier = Modifier,
    detalle: String? = null,
    colorValor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(titulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = colorValor)
            if (detalle != null) {
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Contenedor de cada sección de gráficos. */
@Composable
fun TarjetaDashboard(
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = contenido
        )
    }
}

/** Punto de color + texto: la leyenda no depende solo del color. */
@Composable
fun LeyendaGrafico(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Text(texto, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Dona (PieChart con agujero) con un texto en el centro. Las porciones en 0 no se dibujan. */
@Composable
fun GraficoDona(
    segmentos: List<SegmentoDona>,
    textoCentro: String,
    modifier: Modifier = Modifier
) {
    val colorTexto = MaterialTheme.colorScheme.onSurface.toArgb()
    val colorHueco = MaterialTheme.colorScheme.surface.toArgb()

    AndroidView(
        modifier = modifier,
        factory = { context ->
            PieChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false            // la leyenda se dibuja en Compose
                isDrawHoleEnabled = true
                holeRadius = 62f
                setTransparentCircleRadius(0f)
                setDrawEntryLabels(false)
                setTouchEnabled(false)              // no bloquea el scroll de la pantalla
                setNoDataText("Sin datos")
            }
        },
        update = { grafico ->
            val visibles = segmentos.filter { it.valor > 0 }
            val conjunto = PieDataSet(
                visibles.map { PieEntry(it.valor.toFloat(), it.etiqueta) },
                ""
            ).apply {
                setColors(*visibles.map { it.color.toArgb() }.toIntArray())
                sliceSpace = 2f
                valueTextSize = 12f
                valueTextColor = android.graphics.Color.WHITE
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String = value.toInt().toString()
                }
            }
            grafico.data = PieData(conjunto)
            grafico.setHoleColor(colorHueco)
            grafico.setCenterText(textoCentro)
            grafico.setCenterTextSize(20f)
            grafico.setCenterTextColor(colorTexto)
            grafico.animateY(600)
            grafico.invalidate()
        }
    )
}

/** Barras: monto cobrado vs. monto por cobrar. */
@Composable
fun GraficoBarrasMonto(
    montoCobrado: Double,
    montoPorCobrar: Double,
    modifier: Modifier = Modifier
) {
    val colorCobrado = ExitoVerde.toArgb()
    val colorPorCobrar = MaterialTheme.colorScheme.error.toArgb()
    val colorTexto = MaterialTheme.colorScheme.onSurface.toArgb()
    val colorCuadricula = MaterialTheme.colorScheme.outlineVariant.toArgb()

    AndroidView(
        modifier = modifier,
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(false)
                setDrawGridBackground(false)
                setExtraTopOffset(16f)
                axisRight.isEnabled = false
                axisLeft.axisMinimum = 0f
                axisLeft.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String = "%,.0f".format(Locale.US, value)
                }
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                xAxis.granularity = 1f
                xAxis.valueFormatter = IndexAxisValueFormatter(listOf("Cobrado", "Por cobrar"))
            }
        },
        update = { grafico ->
            val conjunto = BarDataSet(
                listOf(
                    BarEntry(0f, montoCobrado.toFloat()),
                    BarEntry(1f, montoPorCobrar.toFloat())
                ),
                "Monto"
            ).apply {
                setColors(colorCobrado, colorPorCobrar)
                valueTextSize = 12f
                valueTextColor = colorTexto
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String = value.toDouble().comoMonto()
                }
            }
            grafico.data = BarData(conjunto).apply { barWidth = 0.5f }
            grafico.xAxis.textColor = colorTexto
            grafico.axisLeft.textColor = colorTexto
            grafico.axisLeft.gridColor = colorCuadricula
            grafico.axisLeft.axisLineColor = colorCuadricula
            grafico.setFitBars(true)
            grafico.animateY(600)
            grafico.invalidate()
        }
    )
}

/** Líneas: monto cobrado vs. por cobrar en los últimos meses. */
@Composable
fun GraficoLineaIngresos(
    ingresos: List<IngresoMensual>,
    modifier: Modifier = Modifier
) {
    val colorCobrado = ExitoVerde.toArgb()
    val colorPorCobrar = MaterialTheme.colorScheme.error.toArgb()
    val colorTexto = MaterialTheme.colorScheme.onSurface.toArgb()
    val colorCuadricula = MaterialTheme.colorScheme.outlineVariant.toArgb()

    val etiquetas = ingresos.map {
        it.mes.month.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es-PE"))
            .replace(".", "")
            .replaceFirstChar { c -> c.uppercase() }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false            // la leyenda se dibuja en Compose
                setTouchEnabled(false)              // no bloquea el scroll de la pantalla
                setNoDataText("Sin datos")
                setExtraTopOffset(16f)
                axisRight.isEnabled = false
                axisLeft.axisMinimum = 0f
                axisLeft.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String = "%,.0f".format(Locale.US, value)
                }
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                xAxis.granularity = 1f
            }
        },
        update = { grafico ->
            fun conjunto(nombre: String, color: Int, valores: List<Double>) =
                LineDataSet(
                    valores.mapIndexed { i, v -> Entry(i.toFloat(), v.toFloat()) },
                    nombre
                ).apply {
                    this.color = color
                    setCircleColor(color)
                    lineWidth = 2.5f
                    circleRadius = 4f
                    setDrawValues(false)
                }

            grafico.data = LineData(
                conjunto("Cobrado", colorCobrado, ingresos.map { it.montoCobrado }),
                conjunto("Por cobrar", colorPorCobrar, ingresos.map { it.montoPorCobrar })
            )
            grafico.xAxis.valueFormatter = IndexAxisValueFormatter(etiquetas)
            grafico.xAxis.textColor = colorTexto
            grafico.axisLeft.textColor = colorTexto
            grafico.axisLeft.gridColor = colorCuadricula
            grafico.axisLeft.axisLineColor = colorCuadricula
            grafico.animateX(600)
            grafico.invalidate()
        }
    )
}