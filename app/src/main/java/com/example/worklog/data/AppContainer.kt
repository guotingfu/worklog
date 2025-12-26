package com.example.worklog.data

import android.content.Context
import com.example.worklog.data.local.db.AppDatabase
import com.example.worklog.data.local.datastore.SettingsRepository

class AppContainer(private val context: Context) {
    val sessionsRepository by lazy {
        AppDatabase.getDatabase(context).sessionDao()
    }

    val settingsRepository by lazy {
        SettingsRepository(context)
    }
}