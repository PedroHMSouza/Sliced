package com.pedrohmsouza.sliced.ui.workout

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/** Estado da permissão de notificações + ação para pedir (ou abrir as configurações). */
class NotificationAccess(val enabled: Boolean, val request: () -> Unit)

/**
 * No Android 13+ notificações precisam de permissão em tempo de execução. Pedimos ao
 * entrar no treino (é quando o timer passa a importar). Se a pessoa já negou, o sistema
 * não mostra mais o diálogo; aí o botão "Ativar" abre as configurações do app.
 */
@Composable
fun rememberNotificationAccess(): NotificationAccess {
    val context = LocalContext.current
    fun check() = NotificationManagerCompat.from(context).areNotificationsEnabled()
    var enabled by remember { mutableStateOf(check()) }
    var asked by rememberSaveable { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { enabled = check() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { enabled = check() }
    val needsRuntime = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    LaunchedEffect(Unit) {
        if (!enabled && needsRuntime && asked == 0) {
            asked++
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    return NotificationAccess(enabled) {
        if (needsRuntime && asked < 2) {
            asked++
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
