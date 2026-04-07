package com.dubrow.paintrackerfree.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dubrow.paintrackerfree.data.model.PainEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface PainEntryDao {

    @Query("SELECT * FROM pain_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<PainEntry>>

    @Query("SELECT * FROM pain_entries ORDER BY timestamp DESC LIMIT 5")
    fun getRecentEntries(): Flow<List<PainEntry>>

    @Query("SELECT * FROM pain_entries WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    fun getEntriesInRange(startTime: Long, endTime: Long): Flow<List<PainEntry>>

    @Query("SELECT * FROM pain_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): PainEntry?

    @Query("SELECT COUNT(*) FROM pain_entries WHERE timestamp = :timestamp")
    suspend fun countByTimestamp(timestamp: Long): Int

    @Query("SELECT COUNT(*) FROM pain_entries WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay")
    suspend fun countEntriesForDay(startOfDay: Long, endOfDay: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: PainEntry): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entries: List<PainEntry>)

    @Update
    suspend fun update(entry: PainEntry)

    @Delete
    suspend fun delete(entry: PainEntry)

    @Query("DELETE FROM pain_entries")
    suspend fun deleteAll()
}
