package com.pedrohmsouza.sliced.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pedrohmsouza.sliced.SlicedApp

/**
 * Recebe o alarme do fim do descanso e os botões da notificação (+15 s / Pular).
 * Se o processo do app tinha morrido, o Android o recria só para entregar este broadcast;
 * o RestTimer recupera o descanso salvo e decide o que mostrar.
 */
class RestAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val timer = (context.applicationContext as SlicedApp).container.restTimer
        when (intent.action) {
            ACTION_END -> timer.onAlarm(intent.getLongExtra(EXTRA_END, 0L))
            ACTION_ADD_15 -> timer.adjust(15)
            ACTION_SKIP -> timer.stop()
        }
    }

    companion object {
        const val ACTION_END = "com.pedrohmsouza.sliced.REST_END"
        const val ACTION_ADD_15 = "com.pedrohmsouza.sliced.REST_ADD_15"
        const val ACTION_SKIP = "com.pedrohmsouza.sliced.REST_SKIP"
        const val EXTRA_END = "endAt"
        const val EXTRA_LABEL = "label"
    }
}
