package com.pedrohmsouza.sliced

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Classe Application: é criada uma única vez quando o processo do app sobe, antes de
 * qualquer Activity. É o lugar natural para guardar objetos que vivem o app inteiro
 * (banco de dados, repositórios, timer) — ver [AppContainer].
 */
class SlicedApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // ProcessLifecycleOwner avisa quando o app inteiro vai para o fundo (inclusive ao
        // apagar a tela) e quando volta. O timer usa isso para trocar a barra na tela pela
        // notificação com contagem, e vice-versa.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = container.restTimer.onAppForeground()
            override fun onStop(owner: LifecycleOwner) = container.restTimer.onAppBackground()
        })
    }
}
