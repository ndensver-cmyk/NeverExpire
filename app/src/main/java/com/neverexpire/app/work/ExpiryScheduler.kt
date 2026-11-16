package com.neverexpire.app.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.neverexpire.app.data.SettingsRepository
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object ExpiryScheduler {
    private const val WORK_NAME = "daily_expiry_check"

    /** Programa la revisión diaria a la hora elegida en Ajustes. */
    fun schedule(context: Context, replace: Boolean = false) {
        val hour = SettingsRepository.get(context).settings.value.notifyHour
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(LocalTime.of(hour, 0))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMinutes()

        val request = PeriodicWorkRequestBuilder<ExpiryWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            if (replace) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun checkNow(context: Context) {
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<ExpiryWorker>().build())
    }
}
