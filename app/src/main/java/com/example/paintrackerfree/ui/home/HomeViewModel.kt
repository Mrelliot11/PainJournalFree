package com.example.paintrackerfree.ui.home

import androidx.lifecycle.*
import com.example.paintrackerfree.data.model.PainEntry
import com.example.paintrackerfree.data.repository.PainRepository
import com.example.paintrackerfree.util.DateUtils
import kotlinx.coroutines.flow.map

class HomeViewModel(repository: PainRepository) : ViewModel() {

    val recentEntries: LiveData<List<PainEntry>> = repository.getRecentEntries().asLiveData()

    val todayEntries: LiveData<List<PainEntry>> = repository
        .getEntriesInRange(DateUtils.startOfDay(), DateUtils.endOfDay())
        .asLiveData()

    val todayAvgPain: LiveData<Float?> = repository
        .getEntriesInRange(DateUtils.startOfDay(), DateUtils.endOfDay())
        .map { entries -> if (entries.isEmpty()) null else entries.map { it.painLevel }.average().toFloat() }
        .asLiveData()
}
