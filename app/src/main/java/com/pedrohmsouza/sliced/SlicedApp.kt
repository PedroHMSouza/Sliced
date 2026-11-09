package com.pedrohmsouza.sliced

import android.app.Application
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder

/**
 * Classe Application: é criada uma única vez quando o processo do app sobe, antes de
 * qualquer Activity. É o lugar natural para guardar objetos que vivem o app inteiro
 * (banco de dados, repositórios, timer) — ver [AppContainer].
 */
class SlicedApp : Application(), SingletonImageLoader.Factory {
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

    /** Carregador de imagens do Coil com suporte a GIF animado (decodificador nativo no Android 9+). */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(AnimatedImageDecoder.Factory()) else add(GifDecoder.Factory())
            }
            .build()
}
