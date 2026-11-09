package com.pedrohmsouza.sliced.ui

import androidx.annotation.StringRes
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Avisos rápidos (snackbar) que precisam sobreviver à troca de tela — por exemplo,
 * "Treino A salvo" aparece já no Histórico. O ViewModel não tem acesso aos textos do app
 * (`Context`), então manda o id do recurso e os argumentos; a UI monta o texto.
 */
data class UiMessage(@StringRes val res: Int, val args: List<Any> = emptyList())

class UserMessages {
    private val flow = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val messages: SharedFlow<UiMessage> = flow
    fun show(@StringRes res: Int, vararg args: Any) { flow.tryEmit(UiMessage(res, args.toList())) }
}
