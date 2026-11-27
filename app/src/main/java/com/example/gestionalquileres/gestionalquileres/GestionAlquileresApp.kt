package com.example.gestionalquileres

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.gestionalquileres.ui.common.FotoBase64Fetcher
import com.example.gestionalquileres.ui.common.FotoBase64Keyer
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GestionAlquileresApp : Application(), ImageLoaderFactory {

    /** ImageLoader global de Coil, con soporte para fotos Base64 cacheadas. */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                add(FotoBase64Keyer())
                add(FotoBase64Fetcher.Factory())
            }
            .build()
}