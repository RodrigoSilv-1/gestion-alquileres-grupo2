package com.example.gestionalquileres.ui.auth

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.crypto.Cipher

/** Busca la FragmentActivity que envuelve a un Context de Compose. */
fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

object HuellaDactilar {

    /**
     * Muestra el diálogo de huella ligado a [cipher].
     * - [onExito] recibe el cipher ya autenticado (listo para doFinal).
     * - [onError] solo se llama ante errores reales; cancelar o "Usar contraseña" no lo disparan.
     */
    fun autenticar(
        activity: FragmentActivity,
        cipher: Cipher,
        titulo: String,
        subtitulo: String,
        onExito: (Cipher) -> Unit,
        onError: (String) -> Unit
    ) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val autenticado = result.cryptoObject?.cipher
                if (autenticado != null) onExito(autenticado)
                else onError("No se pudo verificar la huella.")
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val cancelado = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                if (!cancelado) onError(errString.toString())
            }
            // onAuthenticationFailed (huella no reconocida): el sistema deja reintentar solo.
        }

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(titulo)
            .setSubtitle(subtitulo)
            .setNegativeButtonText("Usar contraseña")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
            .authenticate(info, BiometricPrompt.CryptoObject(cipher))
    }
}