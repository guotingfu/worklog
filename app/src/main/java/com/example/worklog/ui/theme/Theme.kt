package com.example.worklog.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 使用 Color.kt 中定义的新颜色变量
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    secondary = PrimaryLight,
    tertiary = SuccessGreen,
    background = DarkBackground,
    surface = DarkCard,
    onPrimary = CardWhite,
    onSecondary = CardWhite,
    onTertiary = CardWhite,
    onBackground = TextWhite,
    onSurface = TextWhite,
    error = AlertRed
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = PrimaryLight,
    tertiary = SuccessGreen,
    background = BackgroundGray,
    surface = CardWhite,
    onPrimary = CardWhite,
    onSecondary = CardWhite,
    onTertiary = CardWhite,
    onBackground = TextBlack,
    onSurface = TextBlack,
    error = AlertRed
)

@Composable
fun WorkLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 修改此处：将状态栏颜色设置为背景色 (background)，而非原先的主色 (primary)
            window.statusBarColor = colorScheme.background.toArgb()

            // 设置状态栏图标颜色：深色模式下图标变亮，浅色模式下图标变暗
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}