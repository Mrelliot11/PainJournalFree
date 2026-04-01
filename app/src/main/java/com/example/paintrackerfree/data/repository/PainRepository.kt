package com.example.paintrackerfree.data.repository

import com.example.paintrackerfree.data.db.PainEntryDao
import com.example.paintrackerfree.data.model.PainEntry
import kotlinx.coroutines.flow.Flow

class PainRepository(private val dao: PainEntryDao) {

    fun getAllEntries(): Flow<List<PainEntry>> = dao.getAllEntries()
    fun getRecentEntries(): Flow<List<PainEntry>> = dao.getRecentEntries()
    fun getEntriesInRange(startTime: Long, endTime: Long): Flow<List<PainEntry>> =
        dao.getEntriesInRange(startTime, endTime)

    suspend fun getEntryById(id: Long): PainEntry? = dao.getEntryById(id)
    suspend fun insert(entry: PainEntry): Long = dao.insert(entry)
    suspend fun update(entry: PainEntry) = dao.update(entry)
    suspend fun delete(entry: PainEntry) = dao.delete(entry)
}
