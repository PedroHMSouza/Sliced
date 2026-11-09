package com.pedrohmsouza.sliced.data

import com.pedrohmsouza.sliced.model.Draft
import com.pedrohmsouza.sliced.model.Program
import com.pedrohmsouza.sliced.model.ProgramSource
import com.pedrohmsouza.sliced.model.Session
import kotlinx.coroutines.flow.Flow

/**
 * Repositórios: a "porta de entrada" dos dados para os ViewModels.
 *
 * Os ViewModels conhecem só estas interfaces, nunca o Room diretamente. Vantagens:
 * - dá para testar ViewModels e regras com implementações em memória;
 * - se um dia houver sincronização na nuvem, ela entra aqui sem mexer nas telas.
 *
 * Expomos `Flow`: quem observa recebe a lista nova automaticamente sempre que o banco muda.
 */
interface SessionRepository {
    /** Histórico completo, do treino mais recente para o mais antigo. */
    val sessions: Flow<List<Session>>
    suspend fun save(session: Session)
    suspend fun delete(id: String)
}

/** Treino em andamento (no máximo um). */
interface DraftRepository {
    val draft: Flow<Draft?>
    suspend fun save(draft: Draft)
    suspend fun clear()
}

/** Ficha de treino. Sem edição salva, devolve a ficha inicial. */
interface ProgramRepository {
    val program: Flow<ProgramSource>

    /** Lê a ficha atual, aplica [transform] e grava, uma alteração por vez (nada se perde se duas chegarem juntas). */
    suspend fun update(transform: (Program) -> Program)
}
