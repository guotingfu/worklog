package com.example.worklog

import android.app.Application
import com.example.worklog.data.AppContainer
import com.example.worklog.data.DefaultAppContainer

class WorkLogApplication : Application() {
    // 保持类型为接口 AppContainer
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        // [修复] 实例化具体的实现类 DefaultAppContainer
        container = DefaultAppContainer(this)
    }
}