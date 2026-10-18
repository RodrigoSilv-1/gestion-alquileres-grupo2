package com.example.gestionalquileres.domain.model

// Clase sellada (sealed class) que actúa como un contenedor  para manejar Éxitos o Errores.
sealed class AppResult<out T> {

    // Subclase que representa un resultado exitoso y guarda los datos obtenidos (valor).
    data class Exito<T>(val valor: T) : AppResult<T>()

    // Subclase que representa un fallo y guarda la excepción o error ocurrido.
    data class Error(val excepcion: Exception) : AppResult<Nothing>()
}

// Función de extensión que se ejecuta únicamente si el resultado fue un ÉXITO.
inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Exito) action(valor) // Si es éxito, ejecuta la acción pasándole el dato
    return this // Permite encadenar llamadas (method chaining)
}

// Función de extensión que se ejecuta únicamente si el resultado fue un ERROR.
inline fun <T> AppResult<T>.onFailure(action: (Exception) -> Unit): AppResult<T> {
    if (this is AppResult.Error) action(excepcion) // Si es error, ejecuta la acción pasándole la excepción
    return this // Permite encadenar llamadas
}

/* se creo este contenedor para que los errores de red no afecte la app, guardando y mostrando
los exitos y errores en las operaciones, logrando una comunicacion entre el vm y la base de datos
 */