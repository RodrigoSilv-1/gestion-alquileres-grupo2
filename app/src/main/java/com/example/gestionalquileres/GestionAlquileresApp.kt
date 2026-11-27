package com.example.gestionalquileres

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.gestionalquileres.notificaciones.CanalNotificacion
import com.example.gestionalquileres.ui.common.FotoBase64Fetcher
import com.example.gestionalquileres.ui.common.FotoBase64Keyer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GestionAlquileresApp : Application(), ImageLoaderFactory, Configuration.Provider {

    /** Permite que los Workers reciban sus dependencias de Hilt. */
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        CanalNotificacion.crearTodos(this)
    }

    /** ImageLoader global de Coil, con soporte para fotos Base64 cacheadas. */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                add(FotoBase64Keyer())
                add(FotoBase64Fetcher.Factory())
            }
            .build()
}