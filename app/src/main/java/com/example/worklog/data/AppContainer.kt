package com.example.worklog.data

import android.content.Context
import com.example.worklog.data.local.datastore.SettingsRepository
import com.example.worklog.data.local.db.AppDatabase
import com.example.worklog.data.local.db.SessionDao

// [关键修改] 确保接口中包含 sessionDao
interface AppContainer {
    val sessionsRepository: SessionDao
    val settingsRepository: SettingsRepository
    val sessionDao: SessionDao
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    override val sessionsRepository: SessionDao by lazy {
        database.sessionDao()
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }

    // [关键修改] 实现接口中的 sessionDao，直接指向 sessionsRepository 即可
    override val sessionDao: SessionDao
        get() = sessionsRepository
}