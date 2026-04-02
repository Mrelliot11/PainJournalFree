package com.example.paintrackerfree.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paintrackerfree.data.repository.PainRepository
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: PainRepository) : ViewModel() {

    fun deleteAllEntries() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }
}
