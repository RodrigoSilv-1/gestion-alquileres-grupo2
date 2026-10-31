package com.example.gestionalquileres.domain

/** El ingreso del inquilino debe ser al menos esta cantidad de veces la renta. */
const val FACTOR_SOLVENCIA = 2.0

/** Ingreso mensual mínimo que debe tener un inquilino para alquilar una unidad con esa renta. */
fun ingresoMinimoRequerido(renta: Double): Double = renta * FACTOR_SOLVENCIA

/** True si [ingreso] cubre el mínimo requerido para la [renta] dada. */
fun cumpleSolvencia(ingreso: Double, renta: Double): Boolean =
    ingreso >= ingresoMinimoRequerido(renta)

/** Formatea un monto con 2 decimales y símbolo: 1500.0 -> "$1500.00". */
fun Double.comoMonto(): String = "\$%.2f".format(this)