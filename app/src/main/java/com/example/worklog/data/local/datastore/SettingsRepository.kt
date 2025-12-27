package com.example.worklog.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class Theme {
    LIGHT, DARK, SYSTEM
}

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val ANNUAL_SALARY = stringPreferencesKey("annual_salary")
        val THEME = stringPreferencesKey("theme")
        val START_TIME = stringPreferencesKey("start_time")
        val END_TIME = stringPreferencesKey("end_time")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val WORKING_DAYS = stringSetPreferencesKey("working_days")
        // [新增] 提醒开关 Key
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
    }

    // --- 年薪 ---
    val annualSalaryFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.ANNUAL_SALARY] ?: "" }

    suspend fun updateAnnualSalary(annualSalary: String) {
        context.dataStore.edit { it[PreferencesKeys.ANNUAL_SALARY] = annualSalary }
    }

    // --- 主题 ---
    val themeFlow: Flow<Theme> = context.dataStore.data
        .map { preferences ->
            when (preferences[PreferencesKeys.THEME]) {
                "LIGHT" -> Theme.LIGHT
                "DARK" -> Theme.DARK
                else -> Theme.SYSTEM
            }
        }

    suspend fun updateTheme(theme: Theme) {
        context.dataStore.edit { it[PreferencesKeys.THEME] = theme.name }
    }

    // --- 上班时间 ---
    val startTimeFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.START_TIME] ?: "09:00" }

    suspend fun updateStartTime(startTime: String) {
        context.dataStore.edit { it[PreferencesKeys.START_TIME] = startTime }
    }

    // --- 下班时间 ---
    val endTimeFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.END_TIME] ?: "18:00" }

    suspend fun updateEndTime(endTime: String) {
        context.dataStore.edit { it[PreferencesKeys.END_TIME] = endTime }
    }

    // --- 工作日 ---
    val workingDaysFlow: Flow<Set<DayOfWeek>> = context.dataStore.data
        .map { preferences ->
            val savedSet = preferences[PreferencesKeys.WORKING_DAYS]
            if (savedSet == null) {
                // 默认周一至周五
                setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
            } else {
                savedSet.map { DayOfWeek.valueOf(it) }.toSet()
            }
        }

    suspend fun updateWorkingDays(days: Set<DayOfWeek>) {
        context.dataStore.edit {
            it[PreferencesKeys.WORKING_DAYS] = days.map { day -> day.name }.toSet()
        }
    }

    // --- 引导页状态 ---
    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { it[PreferencesKeys.ONBOARDING_COMPLETED] = true }
    }

    // --- [新增] 提醒开关 ---
    val reminderEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.REMINDER_ENABLED] ?: false }

    suspend fun updateReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.REMINDER_ENABLED] = enabled }
    }
}