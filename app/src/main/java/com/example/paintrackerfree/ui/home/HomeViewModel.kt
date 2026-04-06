package com.example.paintrackerfree.ui.home

import androidx.lifecycle.*
import com.example.paintrackerfree.data.model.PainEntry
import com.example.paintrackerfree.data.repository.PainRepository
import com.example.paintrackerfree.util.DateUtils
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: PainRepository) : ViewModel() {

    var lastDeleted: PainEntry? = null

    fun deleteEntry(entry: PainEntry) {
        lastDeleted = entry
        viewModelScope.launch { repository.delete(entry) }
    }

    fun quickLog(painLevel: Int) {
        viewModelScope.launch {
            repository.insert(PainEntry(painLevel = painLevel))
        }
    }

    fun restoreLastDeleted() {
        lastDeleted?.let { entry ->
            viewModelScope.launch { repository.insert(entry) }
            lastDeleted = null
        }
    }

    val recentEntries: LiveData<List<PainEntry>> = repository.getRecentEntries().asLiveData()

    val todayEntries: LiveData<List<PainEntry>> = repository
        .getEntriesInRange(DateUtils.startOfDay(), DateUtils.endOfDay())
        .asLiveData()

    val todayAvgPain: LiveData<Float?> = repository
        .getEntriesInRange(DateUtils.startOfDay(), DateUtils.endOfDay())
        .map { entries -> if (entries.isEmpty()) null else entries.map { it.painLevel }.average().toFloat() }
        .asLiveData()
}
