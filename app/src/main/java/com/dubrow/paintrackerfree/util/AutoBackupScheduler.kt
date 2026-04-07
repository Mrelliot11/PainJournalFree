package com.dubrow.paintrackerfree.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.dubrow.paintrackerfree.receiver.AutoBackupReceiver

object AutoBackupScheduler {

    private const val REQUEST_CODE = 9001

    fun schedule(context: Context) {
        if (!AutoBackupStore.isEnabled(context)) return

        val intervalMs = when (AutoBackupStore.getFrequency(context)) {
            AutoBackupStore.FREQUENCY_DAILY -> AlarmManager.INTERVAL_DAY
            AutoBackupStore.FREQUENCY_MONTHLY -> AlarmManager.INTERVAL_DAY * 30
            else -> AlarmManager.INTERVAL_DAY * 7 // weekly default
        }

        val pi = pendingIntent(context)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + intervalMs,
            intervalMs,
            pi
        )
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context) = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, AutoBackupReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
