package com.example.gestionalquileres.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

/** Reduce la imagen a [maxDimension] px y la devuelve en Base64 (JPEG). Llamar fuera del hilo principal. */
fun uriABase64(
    context: Context,
    uri: Uri,
    maxDimension: Int = 400,
    calidad: Int = 70
): String? = try {
    val resolver = context.contentResolver

    // 1) Solo leer dimensiones
    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }

    var muestra = 1
    while (limites.outWidth / (muestra * 2) >= maxDimension &&
        limites.outHeight / (muestra * 2) >= maxDimension
    ) {
        muestra *= 2
    }

    // 2) Decodificar ya reducida
    val opciones = BitmapFactory.Options().apply { inSampleSize = muestra }
    val original = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }

    original?.let { bmp ->
        val escala = maxDimension.toFloat() / max(bmp.width, bmp.height)
        val reducido = if (escala < 1f) {
            Bitmap.createScaledBitmap(bmp, (bmp.width * escala).toInt(), (bmp.height * escala).toInt(), true)
        } else bmp

        val salida = ByteArrayOutputStream()
        reducido.compress(Bitmap.CompressFormat.JPEG, calidad, salida)

        if (reducido !== bmp) reducido.recycle()
        bmp.recycle()

        Base64.encodeToString(salida.toByteArray(), Base64.NO_WRAP)
    }
} catch (e: Exception) {
    null
}