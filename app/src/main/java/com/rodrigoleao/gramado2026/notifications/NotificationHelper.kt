package com.rodrigoleao.gramado2026.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.*
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

object NotificationHelper {

    const val CHANNEL_ID         = "gramado_reminders"
    const val CHANNEL_NAME       = "Lembretes da Viagem"
    const val NOTIFICATION_ID    = 1001
    const val WORK_TAG           = "checkin_reminder"

    // Voo de volta: 13 Jun 2026 20:30
    // Lembrete:     10 Jun 2026 20:30 (72h antes)
    private val FLIGHT_DATETIME   = LocalDateTime.of(2026, 6, 13, 20, 30)
    private val REMINDER_DATETIME = FLIGHT_DATETIME.minusHours(72)

    /** Texto formatado da data do lembrete para exibir na UI */
    val reminderDisplay: String get() = "10 Jun · 20h30"

    /** Cria o canal de notificação (chamar no onCreate da MainActivity) */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Lembretes importantes para a viagem a Gramado"
                enableLights(true)
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    /**
     * Agenda a notificação de lembrete de check-in.
     * Retorna true se o agendamento foi bem-sucedido,
     * false se o momento do lembrete já passou.
     */
    fun schedule(context: Context): Boolean {
        val now = LocalDateTime.now()
        val delaySeconds = ChronoUnit.SECONDS.between(now, REMINDER_DATETIME)

        if (delaySeconds <= 0) return false   // lembrete já passou

        val request = OneTimeWorkRequestBuilder<CheckInReminderWorker>()
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .addTag(WORK_TAG)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_TAG, ExistingWorkPolicy.REPLACE, request)

        return true
    }

    /** Cancela o lembrete agendado */
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_TAG)
    }

    /** Verifica se o lembrete já está agendado (consulta síncrona, usar fora da thread principal) */
    fun isScheduled(context: Context): Boolean {
        val infos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(WORK_TAG)
            .get()
        return infos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
    }
}
