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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.worklog.data.local.datastore.Theme
import com.example.worklog.ui.MainScreen
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

            // [新增] 获取键盘控制器和焦点管理器
            val keyboardController = LocalSoftwareKeyboardController.current
            val focusManager = LocalFocusManager.current

            WorkLogTheme(darkTheme = useDarkTheme) {
                // [新增] 全局触摸拦截：点击空白处收起键盘并清除焦点
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
                    MainScreen(settingsViewModel = settingsViewModel)
                }
            }
        }
    }
}