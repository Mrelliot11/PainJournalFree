package com.example.paintrackerfree.ui.history

import androidx.lifecycle.*
import com.example.paintrackerfree.data.model.PainEntry
import com.example.paintrackerfree.data.repository.PainRepository
import com.example.paintrackerfree.util.DateUtils
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

sealed class HistoryItem {
    data class Header(val dateLabel: String) : HistoryItem()
    data class Entry(val entry: PainEntry) : HistoryItem()
}

class HistoryViewModel(private val repository: PainRepository) : ViewModel() {

    var lastDeleted: PainEntry? = null

    val historyItems: LiveData<List<HistoryItem>> = repository.getAllEntries()
        .map { entries ->
            val items = mutableListOf<HistoryItem>()
            var lastHeader = ""
            entries.forEach { entry ->
                val header = DateUtils.toDateHeader(entry.timestamp)
                if (header != lastHeader) {
                    items.add(HistoryItem.Header(header))
                    lastHeader = header
                }
                items.add(HistoryItem.Entry(entry))
            }
            items
        }
        .asLiveData()

    fun deleteEntry(entry: PainEntry) {
        lastDeleted = entry
        viewModelScope.launch { repository.delete(entry) }
    }

    fun restoreLastDeleted() {
        lastDeleted?.let { entry ->
            viewModelScope.launch { repository.insert(entry) }
            lastDeleted = null
        }
    }
}
