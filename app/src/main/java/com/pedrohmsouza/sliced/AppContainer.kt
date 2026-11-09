package com.pedrohmsouza.sliced

import android.content.Context
import com.pedrohmsouza.sliced.data.DraftRepository
import com.pedrohmsouza.sliced.data.InMemoryDraftRepository
import com.pedrohmsouza.sliced.data.InMemorySessionRepository
import com.pedrohmsouza.sliced.data.SessionRepository
import com.pedrohmsouza.sliced.model.ProgramSource
import com.pedrohmsouza.sliced.model.StaticProgram

/**
 * Injeção de dependência manual.
 *
 * Por que não Hilt? O app tem um banco, dois repositórios e um timer: dá para montar
 * tudo à mão neste arquivo, e fica explícito quem cria o quê e quanto tempo cada objeto
 * vive. O Hilt faz exatamente isso, só que gerando código por anotações — vale a pena
 * quando o grafo cresce (vários módulos, escopos por tela). Migrar depois é direto:
 * cada `val` daqui vira um `@Provides`.
 *
 * Uma única instância é criada em [SlicedApp] e vive enquanto o processo viver.
 */
class AppContainer(context: Context) {
    val program: ProgramSource = StaticProgram
    val sessions: SessionRepository = InMemorySessionRepository()
    val drafts: DraftRepository = InMemoryDraftRepository()
    val now: () -> Long = System::currentTimeMillis
}
