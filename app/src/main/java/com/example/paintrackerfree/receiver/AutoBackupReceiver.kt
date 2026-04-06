package com.example.paintrackerfree.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.paintrackerfree.PainTrackerApp
import com.example.paintrackerfree.util.CsvExporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AutoBackupReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entries = (context.applicationContext as PainTrackerApp)
                    .repository
                    .getAllEntries()
                    .first()
                if (entries.isNotEmpty()) {
                    CsvExporter.saveToDownloads(context, entries)
                }
            } finally {
                result.finish()
            }
        }
    }
}
