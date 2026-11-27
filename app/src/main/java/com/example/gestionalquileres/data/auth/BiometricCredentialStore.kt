package com.example.gestionalquileres.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

data class CredencialesGuardadas(val correo: String, val contrasena: String)

/**
 * Guarda correo y contraseña cifrados con una clave del Android Keystore que SOLO se
 * puede usar tras autenticarse con huella. Si se registra una huella nueva en el teléfono,
 * la clave se invalida y hay que volver a activar la función.
 */
@Singleton
class BiometricCredentialStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("huella_credenciales", Context.MODE_PRIVATE)

    /** True si el teléfono tiene sensor de huella y al menos una huella registrada. */
    fun sensorDisponible(): Boolean =
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS

    fun hayCredenciales(): Boolean = prefs.contains(KEY_DATOS) && prefs.contains(KEY_IV)

    /** Cipher listo para CIFRAR. Crea una clave nueva cada vez que se activa la huella. */
    fun cipherParaCifrar(): Cipher {
        eliminarClave()
        crearClave()
        return Cipher.getInstance(TRANSFORMACION).apply { init(Cipher.ENCRYPT_MODE, obtenerClave()) }
    }

    /** Cipher listo para DESCIFRAR. Lanza KeyPermanentlyInvalidatedException si cambiaron las huellas. */
    fun cipherParaDescifrar(): Cipher {
        val iv = Base64.decode(prefs.getString(KEY_IV, null), Base64.NO_WRAP)
        return Cipher.getInstance(TRANSFORMACION).apply {
            init(Cipher.DECRYPT_MODE, obtenerClave(), GCMParameterSpec(128, iv))
        }
    }

    /** [cipher] debe venir ya autenticado por BiometricPrompt. */
    fun guardar(cipher: Cipher, correo: String, contrasena: String) {
        val json = JSONObject().put("c", correo).put("p", contrasena).toString()
        val cifrado = cipher.doFinal(json.toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString(KEY_DATOS, Base64.encodeToString(cifrado, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
    }

    /** [cipher] debe venir ya autenticado por BiometricPrompt. */
    fun leer(cipher: Cipher): CredencialesGuardadas {
        val cifrado = Base64.decode(prefs.getString(KEY_DATOS, null), Base64.NO_WRAP)
        val json = JSONObject(String(cipher.doFinal(cifrado), Charsets.UTF_8))
        return CredencialesGuardadas(json.getString("c"), json.getString("p"))
    }

    fun borrar() {
        prefs.edit().clear().apply()
        eliminarClave()
    }

    // ---------------- Keystore ----------------

    private fun crearClave() {
        val spec = KeyGenParameterSpec.Builder(
            ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)          // exige huella en cada uso
            .setInvalidatedByBiometricEnrollment(true)    // se invalida si se añade otra huella
            .build()

        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }

    private fun obtenerClave(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(ALIAS, null) as SecretKey
    }

    private fun eliminarClave() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(ALIAS)) keyStore.deleteEntry(ALIAS)
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "gestionalquileres_huella"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
        const val KEY_DATOS = "datos"
        const val KEY_IV = "iv"
    }
}