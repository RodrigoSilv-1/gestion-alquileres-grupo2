package com.example.gestionalquileres.domain.model

// Clase sellada (sealed class) que actúa como un contenedor  para manejar Éxitos o Errores.
sealed class AppResult<out T> {

    // Subclase que representa un resultado exitoso y guarda los datos obtenidos (valor).
    data class Exito<T>(val valor: T) : AppResult<T>()

    // Subclase que representa un fallo y guarda la excepción o error ocurrido.
    data class Error(val excepcion: Exception) : AppResult<Nothing>()
}

// es una funcion de extension donde Si la operación salió bien
inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Exito) action(valor)//ejecuta esto con el resultado
    // y Si es éxito, ejecuta la acción pasándole el dato
    return this // Permite encadenar llamadas (method chaining)
}

// Si la operación falló, ejecuta esto con el error y permite seguir de largo
inline fun <T> AppResult<T>.onFailure(action: (Exception) -> Unit): AppResult<T> {
    if (this is AppResult.Error) action(excepcion)// Si es error, ejecuta la
    // acción pasándole la excepción
    return this // Permite encadenar llamadas
}