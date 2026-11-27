package com.example.gestionalquileres.data.remote

import com.example.gestionalquileres.domain.model.AppResult
import retrofit2.HttpException
import java.io.IOException

inline fun <T> safeApiCall(
    apiCall: () -> T
): AppResult<T> {
    return try {
        AppResult.Exito(apiCall())
    } catch (e: HttpException) {
        val cuerpoError = e.response()?.errorBody()?.string()
        android.util.Log.e("API_ERROR", "HTTP ${e.code()}: $cuerpoError", e)
        val mensaje = "Error del servidor (${e.code()}): $cuerpoError"
        AppResult.Error(Exception(mensaje, e))
    } catch (e: IOException) {
        android.util.Log.e("API_ERROR", "Error de red: ${e.message}", e)
        AppResult.Error(Exception("Error de red. Verifica tu conexión a internet.", e))
    } catch (e: Exception) {
        android.util.Log.e("API_ERROR", "Error inesperado: ${e.message}", e)
        AppResult.Error(e)
    }
}