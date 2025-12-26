package com.example.worklog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable // [新增引用]
import androidx.compose.runtime.setValue
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

            // [优化] 使用 rememberSaveable 代替 remember
            // 这样即使旋转屏幕，"本次已关闭弹窗"的状态也会被保留，不会再次弹出
            var showOnboardingDialog by rememberSaveable { mutableStateOf(true) }

            WorkLogTheme(
                darkTheme = when (settingsUiState.theme) {
                    Theme.LIGHT -> false
                    Theme.DARK -> true
                    Theme.SYSTEM -> isSystemInDarkTheme()
                }
            ) {
                MainScreen(settingsViewModel)

                if (!onboardingCompleted && showOnboardingDialog) {
                    OnboardingScreen(
                        onDismiss = { doNotShowAgain ->
                            if (doNotShowAgain) {
                                onboardingViewModel.setOnboardingCompleted()
                            }
                            showOnboardingDialog = false
                        }
                    )
                }
            }
        }
    }
}