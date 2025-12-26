package com.example.worklog.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worklog.data.local.db.Session
import com.example.worklog.data.local.db.SessionDao
import com.example.worklog.data.local.datastore.SettingsRepository
import com.example.worklog.data.local.datastore.Theme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val annualSalary: String = "",
    val theme: Theme = Theme.SYSTEM,
    val startTime: String = "09:00",
    val endTime: String = "18:00",
    val recentSessions: List<Session> = emptyList()
)

class SettingsViewModel(
    private val sessionDao: SessionDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.annualSalaryFlow,
        settingsRepository.themeFlow,
        settingsRepository.startTimeFlow,
        settingsRepository.endTimeFlow,
        sessionDao.getAllSessions() // We'll take the first 10 later in the flow
    ) { salary, theme, startTime, endTime, sessions ->
        SettingsUiState(
            annualSalary = salary,
            theme = theme,
            startTime = startTime,
            endTime = endTime,
            recentSessions = sessions.take(10)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun updateAnnualSalary(salary: String) {
        viewModelScope.launch {
            settingsRepository.updateAnnualSalary(salary)
        }
    }

    fun updateTheme(theme: Theme) {
        viewModelScope.launch {
            settingsRepository.updateTheme(theme)
        }
    }

    fun updateStartTime(time: String) {
        viewModelScope.launch {
            settingsRepository.updateStartTime(time)
        }
    }

    fun updateEndTime(time: String) {
        viewModelScope.launch {
            settingsRepository.updateEndTime(time)
        }
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            sessionDao.deleteSessionById(id)
        }
    }

    fun updateSessionNote(session: Session, note: String) {
        viewModelScope.launch {
            sessionDao.update(session.copy(note = note))
        }
    }
}