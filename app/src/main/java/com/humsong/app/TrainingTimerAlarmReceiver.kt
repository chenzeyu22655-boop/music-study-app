package com.humsong.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class TrainingTimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_TRAINING_TIMER_DONE) return
        val itemId = intent.getStringExtra(EXTRA_TRAINING_ITEM_ID).orEmpty()
        context.alarmVibrate(repeat = true)
        val alertIntent = Intent(context, TimerAlertActivity::class.java).apply {
            putExtra(EXTRA_TRAINING_ITEM_ID, itemId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.showTrainingTimerNotification(alertIntent)
        context.startActivity(alertIntent)
    }

    companion object {
        const val ACTION_TRAINING_TIMER_DONE = "com.humsong.app.action.TRAINING_TIMER_DONE"
        const val EXTRA_TRAINING_ITEM_ID = "training_item_id"
        private const val CHANNEL_ID = "training_timer_alerts"
        private const val NOTIFICATION_ID = 202606071
    }

    private fun Context.showTrainingTimerNotification(alertIntent: Intent) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "训练计时提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "组间休息倒计时结束提醒"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 420, 180, 420, 180, 760)
            }
            notificationManager.createNotificationChannel(channel)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            202606072,
            alertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_training_timer)
            .setContentTitle("训练计时结束")
            .setContentText("休息时间到了，准备继续训练。")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 420, 180, 420, 180, 760))
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }
}
