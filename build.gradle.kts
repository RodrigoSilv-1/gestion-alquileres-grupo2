// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.gms.google-services") version "4.5.0" apply false
    id("com.google.dagger.hilt.android") version "2.55" apply false

    // NUEVO: Agregamos KSP para procesar las inyecciones sin usar Kapt
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
}