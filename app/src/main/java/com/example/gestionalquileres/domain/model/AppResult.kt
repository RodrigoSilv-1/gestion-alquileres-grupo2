package com.example.gestionalquileres.domain.model

sealed class AppResult<out T> {
    data class Exito<T>(val valor: T) : AppResult<T>()
    data class Error(val excepcion: Exception) : AppResult<Nothing>()
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Exito) action(valor)
    return this
}

inline fun <T> AppResult<T>.onFailure(action: (Exception) -> Unit): AppResult<T> {
    if (this is AppResult.Error) action(excepcion)
    return this
}