package com.dubrow.paintrackerfree.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.dubrow.paintrackerfree.data.model.PainEntry
import com.dubrow.paintrackerfree.data.repository.PainRepository
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: PainRepository) : ViewModel() {

    val allEntries: LiveData<List<PainEntry>> = repository.getAllEntries().asLiveData()

    fun deleteAllEntries() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }
}
