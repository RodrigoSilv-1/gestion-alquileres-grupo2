package com.example.gestionalquileres.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction

/**
 * Qué hace Enter en un campo:
 *  - Next → pasa al siguiente campo
 *  - Done → oculta el teclado y ejecuta [onDone]
 */
@Composable
fun accionDeEnter(imeAction: ImeAction, onDone: () -> Unit): () -> Unit {
    val focusManager = LocalFocusManager.current
    val teclado = LocalSoftwareKeyboardController.current
    return {
        if (imeAction == ImeAction.Done) {
            teclado?.hide()
            onDone()
        } else {
            focusManager.moveFocus(FocusDirection.Next)
        }
    }
}

/** Teclado físico / emulador: Tab y Shift+Tab mueven el foco; Enter ejecuta [accionEnter]. */
@Composable
fun Modifier.teclasDeFormulario(accionEnter: () -> Unit): Modifier {
    val focusManager = LocalFocusManager.current
    return onPreviewKeyEvent { evento ->
        if (evento.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (evento.key) {
            Key.Tab -> {
                focusManager.moveFocus(
                    if (evento.isShiftPressed) FocusDirection.Previous else FocusDirection.Next
                )
                true
            }
            Key.Enter, Key.NumPadEnter -> {
                accionEnter()
                true
            }
            else -> false
        }
    }
}