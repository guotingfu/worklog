package com.example.worklog

import android.app.Application
import com.example.worklog.data.AppContainer

class WorkLogApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
