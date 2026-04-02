package com.example.paintrackerfree.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.paintrackerfree.data.repository.PainRepository
import com.example.paintrackerfree.ui.history.HistoryViewModel
import com.example.paintrackerfree.ui.home.HomeViewModel
import com.example.paintrackerfree.ui.logentry.LogEntryViewModel
import com.example.paintrackerfree.ui.reports.ReportsViewModel
import com.example.paintrackerfree.ui.settings.SettingsViewModel

class ViewModelFactory(private val repository: PainRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        HomeViewModel::class.java -> HomeViewModel(repository)
        HistoryViewModel::class.java -> HistoryViewModel(repository)
        ReportsViewModel::class.java -> ReportsViewModel(repository)
        LogEntryViewModel::class.java -> LogEntryViewModel(repository)
        SettingsViewModel::class.java -> SettingsViewModel(repository)
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    } as T
}
