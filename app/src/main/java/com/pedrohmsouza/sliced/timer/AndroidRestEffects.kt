package com.pedrohmsouza.sliced.timer

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.pedrohmsouza.sliced.MainActivity
import com.pedrohmsouza.sliced.R

/** Implementação real dos efeitos do timer: alarme, notificações, som e vibração. */
class AndroidRestEffects(private val context: Context) : RestEffects {

    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val nm = NotificationManagerCompat.from(context)

    init { createChannels() }

    // ---------- Alarme ----------

    override fun scheduleAlarm(endAt: Long, label: String) {
        val pi = alarmIntent(endAt, label)
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        if (exactAllowed) {
            // setAlarmClock é o tipo de alarme mais confiável: o sistema o trata como um
            // despertador (dispara na hora mesmo em Doze/economia de bateria da Samsung).
            // Efeito colateral: o ícone de alarme aparece na barra durante o descanso.
            alarms.setAlarmClock(AlarmManager.AlarmClockInfo(endAt, openAppIntent()), pi)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, pi)
        }
    }

    override fun cancelAlarm() = alarms.cancel(alarmIntent(0, ""))

    private fun alarmIntent(endAt: Long, label: String): PendingIntent = PendingIntent.getBroadcast(
        context, REQ_ALARM,
        Intent(context, RestAlarmReceiver::class.java)
            .setAction(RestAlarmReceiver.ACTION_END)
            .putExtra(RestAlarmReceiver.EXTRA_END, endAt)
            .putExtra(RestAlarmReceiver.EXTRA_LABEL, label),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context, REQ_OPEN,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun actionIntent(action: String, req: Int): PendingIntent = PendingIntent.getBroadcast(
        context, req,
        Intent(context, RestAlarmReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    // ---------- Notificações ----------

    private fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CH_COUNTDOWN, context.getString(R.string.ch_countdown), NotificationManager.IMPORTANCE_LOW).apply {
                description = context.getString(R.string.ch_countdown_desc)
                setShowBadge(false)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CH_DONE, context.getString(R.string.ch_done), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.ch_done_desc)
                enableVibration(true)
                vibrationPattern = VIBRATION
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
                )
            },
        )
    }

    private fun canNotify() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    override fun showCountdown(state: RestState) {
        nm.cancel(ID_DONE)
        if (!canNotify()) return
        val n = NotificationCompat.Builder(context, CH_COUNTDOWN)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(context.getString(R.string.notif_rest_title, state.label))
            .setContentText(context.getString(R.string.notif_rest_text))
            // Cronômetro regressivo desenhado pelo sistema até [endAt].
            .setWhen(state.endAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppIntent())
            .setTimeoutAfter(state.endAt - System.currentTimeMillis() + 2_000)
            // Android 16: pede para virar "Live Update" (no One UI 8, aparece na Now Bar).
            .setRequestPromotedOngoing(true)
            .addAction(0, context.getString(R.string.notif_add15), actionIntent(RestAlarmReceiver.ACTION_ADD_15, REQ_ADD))
            .addAction(0, context.getString(R.string.notif_skip), actionIntent(RestAlarmReceiver.ACTION_SKIP, REQ_SKIP))
            .build()
        notify(ID_COUNTDOWN, n)
    }

    override fun showDone(label: String) {
        nm.cancel(ID_COUNTDOWN)
        if (!canNotify()) { vibrate(); return }
        val n = NotificationCompat.Builder(context, CH_DONE)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(context.getString(R.string.notif_done_title))
            .setContentText(context.getString(R.string.notif_done_text, label))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(VIBRATION)
            .setAutoCancel(true)
            .setTimeoutAfter(120_000)
            .setContentIntent(openAppIntent())
            .build()
        notify(ID_DONE, n)
    }

    @Suppress("MissingPermission") // conferido em canNotify()
    private fun notify(id: Int, n: android.app.Notification) {
        if (canNotify()) nm.notify(id, n)
    }

    override fun cancelNotifications() {
        nm.cancel(ID_COUNTDOWN)
        nm.cancel(ID_DONE)
    }

    // ---------- Som e vibração com o app aberto ----------

    override fun alertInApp() {
        vibrate()
        runCatching {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
            val h = Handler(Looper.getMainLooper())
            listOf(0L, 280L, 560L).forEach { t -> h.postDelayed({ runCatching { tone.startTone(ToneGenerator.TONE_PROP_BEEP, 180) } }, t) }
            h.postDelayed({ tone.release() }, 1_000)
        }
    }

    private fun vibrate() {
        val v: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION") context.getSystemService(Vibrator::class.java)
        }
        runCatching { v?.vibrate(VibrationEffect.createWaveform(VIBRATION, -1)) }
    }

    companion object {
        const val CH_COUNTDOWN = "rest_countdown"
        const val CH_DONE = "rest_done"
        private const val ID_COUNTDOWN = 10
        private const val ID_DONE = 11
        private const val REQ_ALARM = 1
        private const val REQ_OPEN = 2
        private const val REQ_ADD = 3
        private const val REQ_SKIP = 4
        private val VIBRATION = longArrayOf(0, 200, 100, 200)
    }
}
