package com.neverexpire.app.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.neverexpire.app.MainActivity
import com.neverexpire.app.R
import com.neverexpire.app.data.Product

object ExpiryNotifier {
    private const val CHANNEL_ID = "expiry_alerts"
    private const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alertas de vencimiento",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Avisos de productos próximos a vencer" }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun notify(context: Context, expired: List<Product>, soon: List<Product>) {
        if (expired.isEmpty() && soon.isEmpty()) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val title = when {
            soon.isNotEmpty() -> "Consume primero: ${soon.first().name}"
            else -> "Tienes ${expired.size} producto(s) vencido(s)"
        }

        val lines = buildList {
            soon.forEach {
                val d = it.daysLeft()
                val txt = when (d) {
                    0L -> "vence HOY"
                    1L -> "vence mañana"
                    else -> "vence en $d días"
                }
                add("• ${it.name} — $txt")
            }
            expired.forEach { add("• ${it.name} — VENCIDO") }
        }

        val style = NotificationCompat.InboxStyle()
        lines.take(6).forEach { style.addLine(it) }
        if (lines.size > 6) style.setSummaryText("+${lines.size - 6} más")

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(lines.first())
            .setStyle(style)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) { }
    }
}
