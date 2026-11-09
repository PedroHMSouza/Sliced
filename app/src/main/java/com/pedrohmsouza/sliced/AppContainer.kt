package com.pedrohmsouza.sliced

import android.content.Context
import com.pedrohmsouza.sliced.ui.UserMessages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.pedrohmsouza.sliced.data.DraftRepository
import com.pedrohmsouza.sliced.data.RoomDraftRepository
import com.pedrohmsouza.sliced.data.RoomSessionRepository
import com.pedrohmsouza.sliced.data.SessionRepository
import com.pedrohmsouza.sliced.data.db.AppDatabase
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
    // `by lazy`: o banco só é aberto na primeira vez em que alguém precisar dele.
    val database: AppDatabase by lazy { AppDatabase.create(context) }
    val sessions: SessionRepository by lazy { RoomSessionRepository(database.sessionDao()) }
    val drafts: DraftRepository by lazy { RoomDraftRepository(database.draftDao()) }
    val now: () -> Long = System::currentTimeMillis
    val messages = UserMessages()

    /** Escopo que vive o app inteiro: para gravações que não podem ser canceladas ao sair da tela. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
