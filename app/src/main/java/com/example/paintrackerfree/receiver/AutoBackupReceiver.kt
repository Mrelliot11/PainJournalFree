package com.example.paintrackerfree.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.paintrackerfree.PainTrackerApp
import com.example.paintrackerfree.util.AutoBackupStore
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
                if (entries.isEmpty()) return@launch

                val folderUriString = AutoBackupStore.getFolderUri(context)
                if (folderUriString != null) {
                    val folderUri = Uri.parse(folderUriString)
                    val folder = DocumentFile.fromTreeUri(context, folderUri)
                    if (folder != null && folder.canWrite()) {
                        val fileName = "pain_journal_${System.currentTimeMillis()}.csv"
                        val file = folder.createFile("text/csv", fileName)
                        file?.let {
                            context.contentResolver.openOutputStream(it.uri)?.use { stream ->
                                stream.write(CsvExporter.buildCsvBytes(entries))
                            }
                        }
                        return@launch
                    }
                }
                // Fall back to Downloads if no folder set or folder inaccessible
                CsvExporter.saveToDownloads(context, entries)
            } finally {
                result.finish()
            }
        }
    }
}
