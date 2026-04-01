package com.example.paintrackerfree.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pain_entries")
data class PainEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val painLevel: Int,
    val locations: String = "",
    val painTypes: String = "",
    val triggers: String = "",
    val medications: String = "",
    val mood: Int = 3,
    val sleepQuality: Int = 3,
    val notes: String = ""
)
