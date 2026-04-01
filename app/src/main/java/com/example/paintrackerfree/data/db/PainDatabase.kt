package com.example.paintrackerfree.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.paintrackerfree.data.model.PainEntry

@Database(entities = [PainEntry::class], version = 1, exportSchema = false)
abstract class PainDatabase : RoomDatabase() {
    abstract fun painEntryDao(): PainEntryDao

    companion object {
        @Volatile private var INSTANCE: PainDatabase? = null

        fun getInstance(context: Context): PainDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PainDatabase::class.java,
                    "pain_database"
                ).build().also { INSTANCE = it }
            }
    }
}
