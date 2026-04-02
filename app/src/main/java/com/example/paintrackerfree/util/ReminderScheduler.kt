package com.example.paintrackerfree.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.paintrackerfree.receiver.ReminderReceiver
import java.util.Calendar

object ReminderScheduler {

    fun scheduleAll(context: Context) {
        val times = ReminderStore.getTimes(context)
        times.forEachIndexed { index, hhmm ->
            schedule(context, hhmm, requestCode(hhmm))
        }
    }

    fun schedule(context: Context, hhmm: String, reqCode: Int = requestCode(hhmm)) {
        val (hour, minute) = hhmm.split(":").map { it.toInt() }
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_HHMM, hhmm)
        }
        val pi = PendingIntent.getBroadcast(
            context, reqCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, pi)
    }

    fun cancel(context: Context, hhmm: String) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, requestCode(hhmm), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pi)
        pi.cancel()
    }

    fun cancelAll(context: Context) {
        ReminderStore.getTimes(context).forEach { cancel(context, it) }
    }

    /** Stable request code derived from the time string so cancellation works correctly. */
    fun requestCode(hhmm: String): Int {
        val (h, m) = hhmm.split(":").map { it.toInt() }
        return h * 100 + m
    }
}
