package com.example.paintrackerfree.ui.logentry

import androidx.lifecycle.*
import com.example.paintrackerfree.data.model.PainEntry
import com.example.paintrackerfree.data.repository.PainRepository
import com.example.paintrackerfree.util.DateUtils
import kotlinx.coroutines.launch

class LogEntryViewModel(private val repository: PainRepository) : ViewModel() {

    private val _saved = MutableLiveData<Boolean>()
    val saved: LiveData<Boolean> = _saved

    private val _deleted = MutableLiveData<Boolean>()
    val deleted: LiveData<Boolean> = _deleted

    private val _existingEntry = MutableLiveData<PainEntry?>()
    val existingEntry: LiveData<PainEntry?> = _existingEntry

    private val _hasTodayEntry = MutableLiveData<Boolean>()
    val hasTodayEntry: LiveData<Boolean> = _hasTodayEntry

    fun checkTodayEntry() {
        viewModelScope.launch {
            _hasTodayEntry.value = repository.hasEntryToday(
                DateUtils.startOfDay(), DateUtils.endOfDay()
            )
        }
    }

    fun loadEntry(id: Long) {
        viewModelScope.launch {
            _existingEntry.value = repository.getEntryById(id)
        }
    }

    fun loadDuplicate(id: Long) {
        viewModelScope.launch {
            val source = repository.getEntryById(id) ?: return@launch
            // Copy all fields except id (0 = new) and timestamp (use now)
            _existingEntry.value = source.copy(id = 0L, timestamp = System.currentTimeMillis())
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
