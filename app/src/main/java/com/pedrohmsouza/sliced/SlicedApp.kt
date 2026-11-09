package com.pedrohmsouza.sliced

import android.app.Application

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
    }
}
