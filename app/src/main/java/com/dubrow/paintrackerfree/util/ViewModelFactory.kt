package com.dubrow.paintrackerfree.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dubrow.paintrackerfree.data.repository.PainRepository
import com.dubrow.paintrackerfree.ui.history.HistoryViewModel
import com.dubrow.paintrackerfree.ui.home.HomeViewModel
import com.dubrow.paintrackerfree.ui.logentry.LogEntryViewModel
import com.dubrow.paintrackerfree.ui.reports.ReportsViewModel
import com.dubrow.paintrackerfree.ui.settings.SettingsViewModel

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
