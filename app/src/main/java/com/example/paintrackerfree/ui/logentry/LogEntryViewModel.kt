package com.example.paintrackerfree.ui.logentry

import androidx.lifecycle.*
import com.example.paintrackerfree.data.model.PainEntry
import com.example.paintrackerfree.data.repository.PainRepository
import kotlinx.coroutines.launch

class LogEntryViewModel(private val repository: PainRepository) : ViewModel() {

    private val _saved = MutableLiveData<Boolean>()
    val saved: LiveData<Boolean> = _saved

    private val _deleted = MutableLiveData<Boolean>()
    val deleted: LiveData<Boolean> = _deleted

    private val _existingEntry = MutableLiveData<PainEntry?>()
    val existingEntry: LiveData<PainEntry?> = _existingEntry

    fun loadEntry(id: Long) {
        viewModelScope.launch {
            _existingEntry.value = repository.getEntryById(id)
        }
    }

    fun saveEntry(
        entryId: Long,
        timestamp: Long,
        painLevel: Int,
        locations: String,
        painTypes: String,
        triggers: String,
        medications: String,
        mood: Int,
        sleepQuality: Int,
        notes: String
    ) {
        viewModelScope.launch {
            val entry = PainEntry(
                id = entryId,
                timestamp = timestamp,
                painLevel = painLevel,
                locations = locations,
                painTypes = painTypes,
                triggers = triggers,
                medications = medications,
                mood = mood,
                sleepQuality = sleepQuality,
                notes = notes
            )
            if (entryId == 0L) repository.insert(entry) else repository.update(entry)
            _saved.value = true
        }
    }

    fun deleteEntry(entry: PainEntry) {
        viewModelScope.launch {
            repository.delete(entry)
            _deleted.value = true
        }
    }
}
