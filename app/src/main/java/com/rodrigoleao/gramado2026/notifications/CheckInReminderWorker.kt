package com.rodrigoleao.gramado2026.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.rodrigoleao.gramado2026.R

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
            .setContentTitle("✈️ Hora do check-in da volta!")
            .setContentText("Seu voo POA → VCP → REC é daqui a 3 dias (13 Jun às 20h30).")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Faça o check-in agora no app da Azul e adicione os cartões ao Google Wallet.\n\n" +
                    "No app Gramado 2026: Cartões de Embarque → toque em ✏️ e cole os links."
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
