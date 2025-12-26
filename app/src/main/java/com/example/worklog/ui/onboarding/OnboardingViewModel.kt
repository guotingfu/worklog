package com.example.worklog.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worklog.data.local.datastore.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OnboardingViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    val onboardingCompleted: StateFlow<Boolean> = settingsRepository.onboardingCompletedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setOnboardingCompleted() {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted()
        }
    }
}