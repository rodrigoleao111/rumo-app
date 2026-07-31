package com.rodrigoleao.pipa.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.rodrigoleao.pipa.R

class CheckInReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        // Verificar permissão (necessária no Android 13+)
        val hasPermission = ActivityCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return Result.failure()

        val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(applicationContext.getString(R.string.notif_checkin_title))
            .setContentText(applicationContext.getString(R.string.notif_checkin_text))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    applicationContext.getString(R.string.notif_checkin_bigtext)
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(NotificationHelper.NOTIFICATION_ID, notification)

        return Result.success()
    }
}
