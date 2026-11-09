package com.pedrohmsouza.sliced.data

import com.pedrohmsouza.sliced.model.Draft
import com.pedrohmsouza.sliced.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Implementações em memória: usadas nos testes (e como ponto de partida antes do Room). */
class InMemorySessionRepository(initial: List<Session> = emptyList()) : SessionRepository {
    private val state = MutableStateFlow(initial.sortedByDescending { it.startedAt })
    override val sessions: Flow<List<Session>> = state

    override suspend fun save(session: Session) =
        state.update { list -> (list.filterNot { it.id == session.id } + session).sortedByDescending { it.startedAt } }

    override suspend fun delete(id: String) = state.update { list -> list.filterNot { it.id == id } }
}

class InMemoryDraftRepository(initial: Draft? = null) : DraftRepository {
    private val state = MutableStateFlow(initial)
    override val draft: Flow<Draft?> = state
    override suspend fun save(draft: Draft) { state.value = draft }
    override suspend fun clear() { state.value = null }
}
