package com.neverexpire.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neverexpire.app.data.ProductRepository
import com.neverexpire.app.data.SettingsRepository

/** Se ejecuta una vez al día y avisa de lo que vence pronto. */
class ExpiryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repo = ProductRepository.get(applicationContext)
        repo.reload()
        val days = SettingsRepository.get(applicationContext).settings.value.alertDays
        val products = repo.products.value.sortedBy { it.expiryEpochDay }
        val expired = products.filter { it.daysLeft() < 0 }
        val soon = products.filter { it.daysLeft() in 0..days.toLong() }
        ExpiryNotifier.notify(applicationContext, expired, soon)
        return Result.success()
    }
}
