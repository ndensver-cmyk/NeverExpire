package com.neverexpire.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neverexpire.app.data.ProductRepository

/** Se ejecuta una vez al día y avisa de lo que vence en los próximos 3 días. */
class ExpiryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repo = ProductRepository.get(applicationContext)
        repo.reload()
        val products = repo.products.value.sortedBy { it.expiryEpochDay }
        val expired = products.filter { it.daysLeft() < 0 }
        val soon = products.filter { it.daysLeft() in 0..ALERT_DAYS }
        ExpiryNotifier.notify(applicationContext, expired, soon)
        return Result.success()
    }

    companion object {
        const val ALERT_DAYS = 3L
    }
}
