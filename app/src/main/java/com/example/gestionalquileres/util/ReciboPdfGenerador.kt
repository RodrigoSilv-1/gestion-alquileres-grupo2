package com.example.gestionalquileres.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.gestionalquileres.domain.model.Recibo
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object ReciboPdfGenerador {
    fun generar(
        context: Context,
        recibo: Recibo
    ): File {
        val directorio = File(
            context.cacheDir,
            "recibos"
        )

        if (!directorio.exists() && !directorio.mkdirs()) {
            throw IllegalStateException(
                "No se pudo crear la carpeta de recibos."
            )
        }

        val nombreBase = recibo.numeroUnico.ifBlank {
            "recibo_${recibo.idRecibo.ifBlank {
                System.currentTimeMillis().toString()
            }}"
        }

        val nombreSeguro = nombreBase.replace(
            Regex("[^A-Za-z0-9_-]"),
            "_"
        )

        val archivoPdf = File(
            directorio,
            "$nombreSeguro.pdf"
        )

        val documento = PdfDocument()

        try {
            val paginaInfo = PdfDocument.PageInfo.Builder(
                595,
                842,
                1
            ).create()

            val pagina = documento.startPage(paginaInfo)
            val canvas = pagina.canvas
            val pintura = Paint(Paint.ANTI_ALIAS_FLAG)

            var posicionY = 55f

            fun dibujarTexto(
                texto: String,
                tamano: Float = 12f,
                negrita: Boolean = false
            ) {
                pintura.color = Color.BLACK
                pintura.textSize = tamano
                pintura.typeface = if (negrita) {
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
                } else {
                    Typeface.DEFAULT
                }
                pintura.textAlign = Paint.Align.LEFT

                canvas.drawText(
                    texto,
                    50f,
                    posicionY,
                    pintura
                )

                posicionY += tamano + 10f
            }

            fun dibujarLinea() {
                pintura.color = Color.LTGRAY
                pintura.strokeWidth = 1f

                canvas.drawLine(
                    50f,
                    posicionY,
                    545f,
                    posicionY,
                    pintura
                )

                posicionY += 22f
            }

            fun dibujarConcepto(
                nombre: String,
                monto: Double
            ) {
                pintura.color = Color.BLACK
                pintura.textSize = 12f
                pintura.typeface = Typeface.DEFAULT
                pintura.textAlign = Paint.Align.LEFT

                canvas.drawText(
                    nombre,
                    60f,
                    posicionY,
                    pintura
                )

                pintura.textAlign = Paint.Align.RIGHT

                canvas.drawText(
                    formatearMonto(monto),
                    535f,
                    posicionY,
                    pintura
                )

                posicionY += 23f
            }

            dibujarTexto(
                texto = "ALQUISYS",
                tamano = 22f,
                negrita = true
            )

            dibujarTexto(
                texto = "RECIBO DE ALQUILER",
                tamano = 18f,
                negrita = true
            )

            dibujarLinea()

            dibujarTexto(
                "Número: ${recibo.numeroUnico}"
            )

            dibujarTexto(
                "Fecha de emisión: ${recibo.fechaEmision}"
            )

            dibujarTexto(
                "Estado: ${recibo.estadoPago}"
            )

            dibujarLinea()

            dibujarTexto(
                texto = "DATOS DEL ALQUILER",
                tamano = 14f,
                negrita = true
            )

            dibujarTexto(
                "Inquilino: ${
                    recibo.nombreInquilino.ifBlank {
                        "No registrado"
                    }
                }"
            )

            dibujarTexto(
                "DNI: ${
                    recibo.dniInquilino.ifBlank {
                        "No registrado"
                    }
                }"
            )

            dibujarTexto(
                "Contrato: ${recibo.idContrato}"
            )

            dibujarTexto(
                "Unidad: ${
                    recibo.nombreUnidad.ifBlank {
                        "No registrada"
                    }
                }"
            )

            val ubicacion = listOf(
                recibo.direccionInmueble,
                recibo.distritoInmueble
            ).filter {
                it.isNotBlank()
            }.joinToString(" - ")

            dibujarTexto(
                "Ubicación: ${
                    ubicacion.ifBlank {
                        "No registrada"
                    }
                }"
            )

            dibujarLinea()

            dibujarTexto(
                texto = "CONCEPTOS",
                tamano = 14f,
                negrita = true
            )

            recibo.obtenerConceptosCobrar().forEach {
                    concepto ->
                dibujarConcepto(
                    nombre = concepto.first,
                    monto = concepto.second
                )
            }

            dibujarLinea()

            pintura.color = Color.BLACK
            pintura.textSize = 15f
            pintura.typeface = Typeface.create(
                Typeface.DEFAULT,
                Typeface.BOLD
            )
            pintura.textAlign = Paint.Align.LEFT

            canvas.drawText(
                "TOTAL",
                60f,
                posicionY,
                pintura
            )

            pintura.textAlign = Paint.Align.RIGHT

            canvas.drawText(
                formatearMonto(recibo.montoTotal),
                535f,
                posicionY,
                pintura
            )

            documento.finishPage(pagina)

            FileOutputStream(archivoPdf).use {
                    salida ->
                documento.writeTo(salida)
            }

            return archivoPdf
        } finally {
            documento.close()
        }
    }

    private fun formatearMonto(monto: Double): String {
        return String.format(
            Locale.US,
            "S/ %.2f",
            monto
        )
    }
}