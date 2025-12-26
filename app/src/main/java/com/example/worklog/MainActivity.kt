package com.example.worklog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.worklog.data.local.datastore.Theme
import com.example.worklog.ui.MainScreen
import com.example.worklog.ui.onboarding.OnboardingScreen
import com.example.worklog.ui.onboarding.OnboardingViewModel
import com.example.worklog.ui.settings.SettingsViewModel
import com.example.worklog.ui.theme.WorkLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settingsViewModel: SettingsViewModel by viewModels {
            val application = application as WorkLogApplication
            com.example.worklog.ui.ViewModelFactory(application.container)
        }

        val onboardingViewModel: OnboardingViewModel by viewModels {
            val application = application as WorkLogApplication
            com.example.worklog.ui.ViewModelFactory(application.container)
        }

        setContent {
            val settingsUiState by settingsViewModel.uiState.collectAsState()
            val onboardingCompleted by onboardingViewModel.onboardingCompleted.collectAsState()

            WorkLogTheme(
                darkTheme = when (settingsUiState.theme) {
                    Theme.LIGHT -> false
                    Theme.DARK -> true
                    Theme.SYSTEM -> isSystemInDarkTheme()
                }
            ) {
                if (onboardingCompleted) {
                    MainScreen(
                        settingsUiState = settingsUiState,
                        onUpdateAnnualSalary = { settingsViewModel.updateAnnualSalary(it) },
                        onUpdateTheme = { settingsViewModel.updateTheme(it) },
                        onUpdateStartTime = { settingsViewModel.updateStartTime(it) },
                        onUpdateEndTime = { settingsViewModel.updateEndTime(it) },
                        onDeleteSession = { settingsViewModel.deleteSession(it) },
                        onUpdateSessionNote = { session, note -> settingsViewModel.updateSessionNote(session, note) }
                    )
                } else {
                    OnboardingScreen(onCompleted = { onboardingViewModel.setOnboardingCompleted() })
                }
            }
        }
    }
}
