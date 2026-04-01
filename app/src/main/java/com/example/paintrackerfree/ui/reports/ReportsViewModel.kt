package com.example.paintrackerfree.ui.reports

import androidx.lifecycle.*
import com.example.paintrackerfree.data.model.PainEntry
import com.example.paintrackerfree.data.repository.PainRepository
import com.example.paintrackerfree.util.DateUtils
import kotlinx.coroutines.flow.map

data class ReportStats(
    val avgPain: Float,
    val entryCount: Int,
    val maxPain: Int,
    val minPain: Int,
    val topLocation: String,
    val topTrigger: String
)

class ReportsViewModel(private val repository: PainRepository) : ViewModel() {

    val selectedDays = MutableLiveData(30)

    val entries: LiveData<List<PainEntry>> = selectedDays.switchMap { days ->
        repository.getEntriesInRange(DateUtils.daysAgoMs(days), System.currentTimeMillis()).asLiveData()
    }

    val stats: LiveData<ReportStats?> = entries.map { list ->
        if (list.isEmpty()) return@map null
        ReportStats(
            avgPain = list.map { it.painLevel }.average().toFloat(),
            entryCount = list.size,
            maxPain = list.maxOf { it.painLevel },
            minPain = list.minOf { it.painLevel },
            topLocation = topItem(list.flatMap { it.locations.split(",").map(String::trim).filter(String::isNotBlank) }),
            topTrigger = topItem(list.flatMap { it.triggers.split(",").map(String::trim).filter(String::isNotBlank) })
        )
    }

    private fun topItem(items: List<String>): String =
        items.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "—"
}
