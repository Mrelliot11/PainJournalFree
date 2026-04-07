package com.dubrow.paintrackerfree.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dubrow.paintrackerfree.util.AutoBackupScheduler
import com.dubrow.paintrackerfree.util.ReminderScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ReminderScheduler.scheduleAll(context)
            AutoBackupScheduler.schedule(context)
        }
    }
}
