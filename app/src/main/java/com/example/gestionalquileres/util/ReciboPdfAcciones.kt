package com.example.gestionalquileres.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.gestionalquileres.domain.model.Recibo

object ReciboPdfAcciones {

    fun visualizar(
        context: Context,
        recibo: Recibo
    ) {
        try {
            val archivo = ReciboPdfGenerador.generar(
                context = context,
                recibo = recibo
            )

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                archivo
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    uri,
                    "application/pdf"
                )

                clipData = ClipData.newRawUri(
                    "Recibo PDF",
                    uri
                )

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            context.startActivity(intent)
        } catch (error: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "No hay una aplicación disponible para visualizar PDF.",
                Toast.LENGTH_LONG
            ).show()
        } catch (error: Exception) {
            Toast.makeText(
                context,
                "No se pudo generar o abrir el recibo PDF.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun compartir(
        context: Context,
        recibo: Recibo
    ) {
        try {
            val archivo = ReciboPdfGenerador.generar(
                context = context,
                recibo = recibo
            )

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                archivo
            )

            val intentCompartir = Intent(
                Intent.ACTION_SEND
            ).apply {
                type = "application/pdf"

                putExtra(
                    Intent.EXTRA_STREAM,
                    uri
                )

                clipData = ClipData.newRawUri(
                    "Recibo PDF",
                    uri
                )

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            context.startActivity(
                Intent.createChooser(
                    intentCompartir,
                    "Compartir recibo"
                )
            )
        } catch (error: Exception) {
            Toast.makeText(
                context,
                "No se pudo generar o compartir el recibo PDF.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}