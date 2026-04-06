package com.example.paintrackerfree.ui.history

import androidx.lifecycle.*
import com.example.paintrackerfree.data.model.PainEntry
import com.example.paintrackerfree.data.repository.PainRepository
import com.example.paintrackerfree.util.DateUtils
import kotlinx.coroutines.launch

sealed class HistoryItem {
    data class Header(val dateLabel: String) : HistoryItem()
    data class Entry(val entry: PainEntry) : HistoryItem()
}

class HistoryViewModel(private val repository: PainRepository) : ViewModel() {

    var lastDeleted: PainEntry? = null

    private val allEntries: LiveData<List<PainEntry>> = repository.getAllEntries().asLiveData()

    val searchQuery = MutableLiveData("")
    val minPain = MutableLiveData(0)
    val maxPain = MutableLiveData(10)
    val locationFilter = MutableLiveData<String?>(null)
    val triggerFilter = MutableLiveData<String?>(null)

    /** Distinct locations across all entries, for the filter chip list. */
    val availableLocations: LiveData<List<String>> = allEntries.map { list ->
        list.flatMap { it.locations.split(",").map(String::trim).filter(String::isNotBlank) }
            .distinct().sorted()
    }

    /** Distinct triggers across all entries, for the filter chip list. */
    val availableTriggers: LiveData<List<String>> = allEntries.map { list ->
        list.flatMap { it.triggers.split(",").map(String::trim).filter(String::isNotBlank) }
            .distinct().sorted()
    }

    val historyItems: LiveData<List<HistoryItem>> = MediatorLiveData<List<HistoryItem>>().apply {
        fun recompute() {
            val entries = allEntries.value ?: emptyList()
            val query = searchQuery.value.orEmpty().trim().lowercase()
            val min = minPain.value ?: 0
            val max = maxPain.value ?: 10
            val location = locationFilter.value
            val trigger = triggerFilter.value

            val filtered = entries.filter { entry ->
                val painOk = entry.painLevel in min..max
                val locationOk = location == null ||
                    entry.locations.split(",").map(String::trim).any { it.equals(location, ignoreCase = true) }
                val triggerOk = trigger == null ||
                    entry.triggers.split(",").map(String::trim).any { it.equals(trigger, ignoreCase = true) }
                val queryOk = query.isEmpty() ||
                    entry.locations.lowercase().contains(query) ||
                    entry.painTypes.lowercase().contains(query) ||
                    entry.triggers.lowercase().contains(query) ||
                    entry.medications.lowercase().contains(query) ||
                    entry.notes.lowercase().contains(query)
                painOk && locationOk && triggerOk && queryOk
            }

            val items = mutableListOf<HistoryItem>()
            var lastHeader = ""
            filtered.forEach { entry ->
                val header = DateUtils.toDateHeader(entry.timestamp)
                if (header != lastHeader) {
                    items.add(HistoryItem.Header(header))
                    lastHeader = header
                }
                items.add(HistoryItem.Entry(entry))
            }
            value = items
        }
        addSource(allEntries) { recompute() }
        addSource(searchQuery) { recompute() }
        addSource(minPain) { recompute() }
        addSource(maxPain) { recompute() }
        addSource(locationFilter) { recompute() }
        addSource(triggerFilter) { recompute() }
    }

    /** Count of entry rows currently visible (after filters applied). */
    val entryCount: LiveData<Int> = historyItems.map { items ->
        items.count { it is HistoryItem.Entry }
    }

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

    fun clearFilters() {
        searchQuery.value = ""
        minPain.value = 0
        maxPain.value = 10
        locationFilter.value = null
        triggerFilter.value = null
    }
}
