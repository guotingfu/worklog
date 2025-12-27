package com.example.worklog.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.worklog.data.AppContainer
import com.example.worklog.ui.home.HomeViewModel
import com.example.worklog.ui.onboarding.OnboardingViewModel
import com.example.worklog.ui.settings.SettingsViewModel
import com.example.worklog.ui.stats.StatsViewModel

class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST") // [修改] 添加此注解以消除转换警告
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(container.sessionsRepository, container.settingsRepository) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(container.sessionsRepository, container.settingsRepository) as T
            }
            modelClass.isAssignableFrom(StatsViewModel::class.java) -> {
                StatsViewModel(container.sessionsRepository, container.settingsRepository) as T
            }
            modelClass.isAssignableFrom(OnboardingViewModel::class.java) -> {
                OnboardingViewModel(container.settingsRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}