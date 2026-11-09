package com.pedrohmsouza.sliced.ui

import kotlinx.serialization.Serializable

/**
 * Rotas da navegação. Desde o Navigation 2.8 as rotas podem ser classes @Serializable
 * ("type-safe navigation"): o compilador confere os argumentos, sem montar strings
 * como "history?open=123" na mão.
 */
@Serializable data object HomeRoute

/** [openSessionId]: treino que já abre expandido (o recém-salvo). */
@Serializable data class HistoryRoute(val openSessionId: String? = null)

@Serializable data object WorkoutRoute
