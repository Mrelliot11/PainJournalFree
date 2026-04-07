package com.dubrow.paintrackerfree

import android.app.Application
import com.dubrow.paintrackerfree.data.db.PainDatabase
import com.dubrow.paintrackerfree.data.repository.PainRepository
import com.dubrow.paintrackerfree.util.ThemeStore

class PainTrackerApp : Application() {
    val database by lazy { PainDatabase.getInstance(this) }
    val repository by lazy { PainRepository(database.painEntryDao()) }

    override fun onCreate() {
        super.onCreate()
        ThemeStore.applyStored(this)
    }
}
