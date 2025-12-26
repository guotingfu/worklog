package com.example.worklog.ui.theme

import androidx.compose.ui.graphics.Color

// === 新定义的高级配色 ===
val PrimaryBlue = Color(0xFF2D60FF)      // 主色：科技蓝
val PrimaryLight = Color(0xFF5B8CFF)     // 浅蓝：用于渐变
val SuccessGreen = Color(0xFF00C853)     // 绿色：正向指标
val AlertRed = Color(0xFFFF4D4F)         // 红色：强调/停止
val AlertLight = Color(0xFFFF7875)       // 浅红：用于渐变

// Light Theme
val BackgroundGray = Color(0xFFF5F7FA)   // 背景：极淡的灰白
val CardWhite = Color(0xFFFFFFFF)        // 卡片：纯白
val TextBlack = Color(0xFF1A1C1E)        // 文字：深黑
val TextGray = Color(0xFF8C9199)         // 文字：浅灰

// Dark Theme (based on images)
val DarkBackground = Color(0xFF1A1C1E)
val DarkCard = Color(0xFF252A30) // A bit lighter
val TextWhite = Color(0xFFFFFFFF)
val TextWhiteGray = Color(0xFFB0B3B8)
val StatusGreen = Color(0xFF4CAF50)
val StatusGray = Color(0xFF616161)


// === 覆盖默认颜色 (为了兼容 Theme.kt 不报错) ===
val Purple80 = PrimaryBlue
val PurpleGrey80 = TextGray
val Pink80 = AlertRed

val Purple40 = PrimaryBlue
val PurpleGrey40 = TextGray
val Pink40 = AlertRed