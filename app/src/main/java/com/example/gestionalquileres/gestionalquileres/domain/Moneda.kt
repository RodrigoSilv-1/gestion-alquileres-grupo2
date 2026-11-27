package com.example.gestionalquileres.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/** Símbolo de la moneda de toda la app (soles peruanos). Cámbialo aquí y cambia en todas las pantallas. */
const val SIMBOLO_MONEDA = "S/"

/** Tasa del IGV, como texto para que BigDecimal la use exacta. */
const val TASA_IGV = "0.18"

/** Formatea un monto con separador de miles y 2 decimales: 1500.0 -> "S/ 1,500.00". */
fun Double.comoMonto(): String = "$SIMBOLO_MONEDA ${"%,.2f".format(Locale.US, this)}"

/** Usa la representación decimal del Double ("0.1"), no su valor binario aproximado. */
private fun Double.aDecimal(): BigDecimal = BigDecimal.valueOf(this)

private fun BigDecimal.aDosDecimales(): BigDecimal = setScale(2, RoundingMode.HALF_UP)

/** Redondea a 2 decimales (mitad hacia arriba) y devuelve un Double limpio, listo para guardar. */
fun Double.redondearMonto(): Double = aDecimal().aDosDecimales().toDouble()

/** Texto del formulario ("12,5" o "12.5") -> monto redondeado. Vacío o inválido -> 0.0. */
fun String.comoMontoOCero(): Double =
    replace(',', '.').trim().toDoubleOrNull()
        ?.takeIf { it.isFinite() }
        ?.redondearMonto()
        ?: 0.0

/** IGV de la renta, calculado con BigDecimal y redondeado a 2 decimales. */
fun calcularIgv(renta: Double): Double =
    renta.aDecimal().aDosDecimales()
        .multiply(BigDecimal(TASA_IGV))
        .aDosDecimales()
        .toDouble()

/** Suma montos sin error de coma flotante; cada monto se redondea a 2 decimales antes de sumar. */
fun sumarMontos(vararg montos: Double): Double =
    montos.fold(BigDecimal.ZERO) { acumulado, monto -> acumulado + monto.aDecimal().aDosDecimales() }
        .aDosDecimales()
        .toDouble()

/** Deja solo dígitos y un separador decimal (se acepta "." o ","), con máximo 2 decimales. */
fun limpiarMonto(texto: String): String {
    val resultado = StringBuilder()
    var hayDecimal = false
    var decimales = 0
    for (c in texto) {
        when {
            c.isDigit() -> when {
                !hayDecimal -> resultado.append(c)
                decimales < 2 -> { resultado.append(c); decimales++ }
            }
            (c == '.' || c == ',') && !hayDecimal -> { hayDecimal = true; resultado.append('.') }
        }
    }
    return resultado.toString().take(10)
}