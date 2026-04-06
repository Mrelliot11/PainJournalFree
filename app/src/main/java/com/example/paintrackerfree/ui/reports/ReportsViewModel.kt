package com.example.paintrackerfree.ui.reports

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.example.paintrackerfree.data.model.PainEntry
import com.example.paintrackerfree.data.repository.PainRepository
import com.example.paintrackerfree.util.DateUtils

data class ReportStats(
    val avgPain: Float,
    val entryCount: Int,
    val maxPain: Int,
    val minPain: Int,
    val topLocation: String,
    val topTrigger: String
)

data class InsightCategory(
    val title: String,
    val items: List<Pair<String, Int>>  // label to count, sorted descending
)

class ReportsViewModel(private val repository: PainRepository) : ViewModel() {

    val selectedDays = MutableLiveData(30)

    /** All entries in the selected date range, unfiltered. */
    val entries: LiveData<List<PainEntry>> = selectedDays.switchMap { days ->
        repository.getEntriesInRange(DateUtils.daysAgoMs(days), System.currentTimeMillis()).asLiveData()
    }

    /** Sorted, distinct pain types present in the current date range. Empty strings excluded. */
    val availablePainTypes: LiveData<List<String>> = entries.map { list ->
        list.flatMap { it.painTypes.split(",").map(String::trim).filter(String::isNotBlank) }
            .distinct()
            .sorted()
    }

    /** The pain type chip the user has selected, or null for "All". */
    val selectedPainType = MutableLiveData<String?>(null)

    /** Entries filtered by selectedPainType — used for chart, stats, and insights. */
    val filteredEntries: LiveData<List<PainEntry>> = MediatorLiveData<List<PainEntry>>().apply {
        fun recompute() {
            val list = entries.value ?: emptyList()
            val type = selectedPainType.value
            value = if (type == null) list
            else list.filter { entry ->
                entry.painTypes.split(",").map(String::trim).any { it.equals(type, ignoreCase = true) }
            }
        }
        addSource(entries) { recompute() }
        addSource(selectedPainType) { recompute() }
    }

    val stats: LiveData<ReportStats?> = filteredEntries.map { list ->
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

    val triggerInsights: LiveData<List<InsightCategory>> = filteredEntries.map { list ->
        val highPain = list.filter { it.painLevel >= 7 }
        if (highPain.isEmpty()) return@map emptyList()

        fun csvCounts(selector: (PainEntry) -> String): List<Pair<String, Int>> =
            highPain.flatMap { selector(it).split(",").map(String::trim).filter(String::isNotBlank) }
                .groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }
                .map { it.key to it.value }

        fun moodLabel(value: Int) = when (value) {
            1 -> "Very Low"
            2 -> "Low"
            3 -> "Neutral"
            4 -> "Good"
            else -> "Great"
        }
        fun sleepLabel(value: Int) = when (value) {
            1 -> "Very Poor"
            2 -> "Poor"
            3 -> "Fair"
            4 -> "Good"
            else -> "Great"
        }

        val moodCounts = highPain.map { moodLabel(it.mood) }
            .groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }
            .map { it.key to it.value }

        val sleepCounts = highPain.map { sleepLabel(it.sleepQuality) }
            .groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }
            .map { it.key to it.value }

        listOf(
            InsightCategory("Triggers", csvCounts { it.triggers }),
            InsightCategory("Locations", csvCounts { it.locations }),
            InsightCategory("Pain Types", csvCounts { it.painTypes }),
            InsightCategory("Medications", csvCounts { it.medications }),
            InsightCategory("Mood", moodCounts),
            InsightCategory("Sleep Quality", sleepCounts)
        ).filter { it.items.isNotEmpty() }
    }

    private fun topItem(items: List<String>): String =
        items.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "—"
}
