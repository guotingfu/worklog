package com.example.worklog

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import com.example.worklog.data.local.datastore.Theme
import com.example.worklog.ui.MainScreen
import com.example.worklog.ui.onboarding.OnboardingScreen
import com.example.worklog.ui.onboarding.OnboardingViewModel
import com.example.worklog.ui.settings.SettingsViewModel
import com.example.worklog.ui.theme.WorkLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

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

            var showOnboardingDialog by rememberSaveable { mutableStateOf(true) }

            WorkLogTheme(
                darkTheme = when (settingsUiState.theme) {
                    Theme.LIGHT -> false
                    Theme.DARK -> true
                    Theme.SYSTEM -> isSystemInDarkTheme()
                }
            ) {
                val focusManager = LocalFocusManager.current
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                focusManager.clearFocus()
                            })
                        },
                    color = MaterialTheme.colorScheme.background,
                    shape = RectangleShape
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
}