package com.example.paintrackerfree.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.paintrackerfree.MainActivity
import com.example.paintrackerfree.R
import com.example.paintrackerfree.util.ReminderScheduler

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Pain Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Daily reminders to log your pain level"
                }
            )
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_LOG_ENTRY, true)
        }
        val pi = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hhmm = intent.getStringExtra(EXTRA_HHMM) ?: ""
        val notifId = if (hhmm.isNotBlank()) {
            val (h, m) = hhmm.split(":").map { it.toInt() }
            h * 100 + m
        } else 0

        // Reschedule for tomorrow — setExactAndAllowWhileIdle is one-shot
        if (hhmm.isNotBlank()) {
            ReminderScheduler.schedule(context, hhmm)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(R.string.reminder_body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        nm.notify(notifId, notification)
    }

    companion object {
        const val CHANNEL_ID = "pain_reminders"
        const val EXTRA_HHMM = "extra_hhmm"
    }
}
