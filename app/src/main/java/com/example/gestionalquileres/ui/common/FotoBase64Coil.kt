package com.example.gestionalquileres.ui.common

import android.util.Base64
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.key.Keyer
import coil.request.Options
import okio.Buffer

/** Modelo que se le pasa a Coil en lugar de decodificar el Base64 a mano. */
data class FotoBase64(val base64: String)

/** Clave estable de caché: mientras la foto no cambie, Coil reutiliza el bitmap ya decodificado. */
class FotoBase64Keyer : Keyer<FotoBase64> {
    override fun key(data: FotoBase64, options: Options): String =
        "foto-b64:${data.base64.length}:${data.base64.hashCode()}"
}

/** Solo se ejecuta cuando la foto NO está en la caché de memoria de Coil. */
class FotoBase64Fetcher(
    private val data: FotoBase64,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val bytes = Base64.decode(data.base64, Base64.DEFAULT)
        return SourceResult(
            source = ImageSource(Buffer().apply { write(bytes) }, options.context),
            mimeType = null,
            dataSource = DataSource.MEMORY
        )
    }

    class Factory : Fetcher.Factory<FotoBase64> {
        override fun create(data: FotoBase64, options: Options, imageLoader: ImageLoader): Fetcher =
            FotoBase64Fetcher(data, options)
    }
}