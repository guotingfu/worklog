package com.example.worklog.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class Theme {
    LIGHT,
    DARK,
    SYSTEM
}

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val ANNUAL_SALARY = stringPreferencesKey("annual_salary")
        val THEME = stringPreferencesKey("theme")
        val START_TIME = stringPreferencesKey("start_time")
        val END_TIME = stringPreferencesKey("end_time")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val annualSalaryFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.ANNUAL_SALARY] ?: ""
        }

    suspend fun updateAnnualSalary(annualSalary: String) {
        context.dataStore.edit {
            it[PreferencesKeys.ANNUAL_SALARY] = annualSalary
        }
    }

    val themeFlow: Flow<Theme> = context.dataStore.data
        .map { preferences ->
            when (preferences[PreferencesKeys.THEME]) {
                "LIGHT" -> Theme.LIGHT
                "DARK" -> Theme.DARK
                else -> Theme.SYSTEM
            }
        }

    suspend fun updateTheme(theme: Theme) {
        context.dataStore.edit {
            it[PreferencesKeys.THEME] = theme.name
        }
    }

    val startTimeFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.START_TIME] ?: "09:00"
        }

    suspend fun updateStartTime(startTime: String) {
        context.dataStore.edit {
            it[PreferencesKeys.START_TIME] = startTime
        }
    }

    val endTimeFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.END_TIME] ?: "18:00"
        }

    suspend fun updateEndTime(endTime: String) {
        context.dataStore.edit {
            it[PreferencesKeys.END_TIME] = endTime
        }
    }

    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
        }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit {
            it[PreferencesKeys.ONBOARDING_COMPLETED] = true
        }
    }
}
