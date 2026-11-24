package com.example.gestionalquileres.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

/** Foto ya procesada: miniatura para las listas y versión grande para verla en detalle (ambas Base64). */
data class FotoProcesada(val miniatura: String, val completa: String)

private const val LADO_MINIATURA = 160
private const val CALIDAD_MINIATURA = 60
private const val MAX_BYTES_MINIATURA = 12 * 1024

private const val LADO_COMPLETA = 800
private const val CALIDAD_COMPLETA = 70
private const val MAX_BYTES_COMPLETA = 150 * 1024

private const val CALIDAD_MINIMA = 40
private const val LADO_MINIMO = 64

/** WebP con pérdida: ~30 % más liviano que JPEG a igual calidad visual. */
@Suppress("DEPRECATION")
private val FORMATO_WEBP: Bitmap.CompressFormat =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY
    else Bitmap.CompressFormat.WEBP

/** Genera miniatura y foto grande. Se llama fuera del hilo principal. Devuelve null si no se pudo leer. */
fun uriAFotoProcesada(context: Context, uri: Uri): FotoProcesada? {
    return try {
        val origen = decodificarReducida(context, uri, LADO_COMPLETA) ?: return null
        try {
            val completa = comprimir(origen, LADO_COMPLETA, CALIDAD_COMPLETA, MAX_BYTES_COMPLETA)
            val miniatura = comprimir(origen, LADO_MINIATURA, CALIDAD_MINIATURA, MAX_BYTES_MINIATURA)
            FotoProcesada(
                miniatura = Base64.encodeToString(miniatura, Base64.NO_WRAP),
                completa = Base64.encodeToString(completa, Base64.NO_WRAP)
            )
        } finally {
            origen.recycle()
        }
    } catch (e: Exception) {
        null
    }
}

/** Decodifica la imagen ya reducida (inSampleSize) para no cargar la foto original completa en memoria. */
private fun decodificarReducida(context: Context, uri: Uri, ladoMaximo: Int): Bitmap? {
    val resolver = context.contentResolver

    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }

    var muestra = 1
    while (limites.outWidth / (muestra * 2) >= ladoMaximo &&
        limites.outHeight / (muestra * 2) >= ladoMaximo
    ) {
        muestra *= 2
    }

    val opciones = BitmapFactory.Options().apply { inSampleSize = muestra }
    return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
}

private fun escalar(origen: Bitmap, lado: Int): Bitmap {
    val mayor = max(origen.width, origen.height)
    if (mayor <= lado) return origen
    val escala = lado.toFloat() / mayor
    return Bitmap.createScaledBitmap(
        origen,
        (origen.width * escala).toInt().coerceAtLeast(1),
        (origen.height * escala).toInt().coerceAtLeast(1),
        true
    )
}

/**
 * Comprime hasta que el resultado pese como máximo [maxBytes]: primero baja la calidad
 * de 10 en 10 y, si no basta, reduce el tamaño un 20 %. Garantiza el tope de tamaño.
 */
private fun comprimir(origen: Bitmap, ladoMaximo: Int, calidadInicial: Int, maxBytes: Int): ByteArray {
    var lado = ladoMaximo
    var calidad = calidadInicial

    while (true) {
        val bitmap = escalar(origen, lado)
        val salida = ByteArrayOutputStream()
        bitmap.compress(FORMATO_WEBP, calidad, salida)
        if (bitmap !== origen) bitmap.recycle()
        val bytes = salida.toByteArray()

        val enElMinimo = calidad <= CALIDAD_MINIMA && lado <= LADO_MINIMO
        if (bytes.size <= maxBytes || enElMinimo) return bytes

        if (calidad > CALIDAD_MINIMA) calidad = max(CALIDAD_MINIMA, calidad - 10)
        else lado = max(LADO_MINIMO, (lado * 0.8f).toInt())
    }
}