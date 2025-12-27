package com.example.worklog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.worklog.data.local.datastore.Theme
import com.example.worklog.ui.MainScreen
import com.example.worklog.ui.onboarding.OnboardingScreen
import com.example.worklog.ui.settings.SettingsViewModel
import com.example.worklog.ui.theme.WorkLogTheme
import com.example.worklog.ui.viewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory())
            val uiState by settingsViewModel.uiState.collectAsState()

            val useDarkTheme = when (uiState.theme) {
                Theme.LIGHT -> false
                Theme.DARK -> true
                Theme.SYSTEM -> isSystemInDarkTheme()
            }

            val keyboardController = LocalSoftwareKeyboardController.current
            val focusManager = LocalFocusManager.current

            WorkLogTheme(darkTheme = useDarkTheme) {
                // [修复] 引入一个仅在当前会话有效的状态，记录浮窗是否已被关闭
                var dialogDismissed by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            })
                        }
                ) {
                    // 始终渲染主屏幕，避免闪烁
                    MainScreen(settingsViewModel = settingsViewModel)

                    // [修复] 如果永久设置未完成，且当前会话未关闭，则显示浮窗
                    if (!uiState.onboardingCompleted && !dialogDismissed) {
                        OnboardingScreen(
                            onDismiss = { doNotShowAgain ->
                                // 如果用户勾选了“不再显示”，则永久保存该设置
                                if (doNotShowAgain) {
                                    settingsViewModel.setOnboardingCompleted()
                                }
                                // 无论如何，在当前会话中关闭浮窗
                                dialogDismissed = true
                            }
                        )
                    }
                }
            }
        }
    }
}