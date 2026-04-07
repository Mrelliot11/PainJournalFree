package com.dubrow.paintrackerfree.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.dubrow.paintrackerfree.PainTrackerApp
import com.dubrow.paintrackerfree.util.AutoBackupStore
import com.dubrow.paintrackerfree.util.CsvExporter
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
                    val folderUri = folderUriString.toUri()
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
