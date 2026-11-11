package com.m9r1u5.datatracker.work

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.m9r1u5.datatracker.MainActivity
import com.m9r1u5.datatracker.R
import com.m9r1u5.datatracker.util.Perms

object Notifs {
    const val ALERTS = "alerts"
    const val SUMMARY = "summary"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(ALERTS, "Data alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Daily limit, per-app limit and bundle warnings"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(SUMMARY, "Daily summary", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "A short summary of today's data use"
            }
        )
    }

    @SuppressLint("MissingPermission")
    fun show(context: Context, id: Int, channel: String, title: String, text: String) {
        if (!Perms.hasNotifications(context)) return
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (e: SecurityException) {
            // Notifications switched off — nothing to do.
        }
    }
}
