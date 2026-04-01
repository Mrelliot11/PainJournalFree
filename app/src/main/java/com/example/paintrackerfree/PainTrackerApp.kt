package com.example.paintrackerfree

import android.app.Application
import com.example.paintrackerfree.data.db.PainDatabase
import com.example.paintrackerfree.data.repository.PainRepository

class PainTrackerApp : Application() {
    val database by lazy { PainDatabase.getInstance(this) }
    val repository by lazy { PainRepository(database.painEntryDao()) }
}
