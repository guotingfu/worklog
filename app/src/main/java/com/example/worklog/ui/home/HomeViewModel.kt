package com.example.worklog.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worklog.data.local.db.Session
import com.example.worklog.data.local.db.SessionDao
import com.example.worklog.data.local.datastore.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

data class HomeUiState(
    val isWorking: Boolean = false,
    val timer: String = "00:00:00",
    val annualSalary: String = "",
    val isSalaryVisible: Boolean = true,
    val latestSession: Session? = null
)

class HomeViewModel(
    private val sessionDao: SessionDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        combine(
            sessionDao.getLatestSession(),
            settingsRepository.annualSalaryFlow
        ) { session, salary ->
            _uiState.value = _uiState.value.copy(
                latestSession = session,
                isWorking = session != null && session.endTime == null,
                annualSalary = salary
            )
            if (_uiState.value.isWorking) {
                startTimer()
            } else {
                stopTimer()
                _uiState.value = _uiState.value.copy(timer = "00:00:00")
            }
        }.launchIn(viewModelScope)
    }

    fun toggleWork() {
        viewModelScope.launch {
            if (_uiState.value.isWorking) {
                _uiState.value.latestSession?.let {
                    sessionDao.update(it.copy(endTime = Instant.now()))
                }
            } else {
                sessionDao.insert(Session(startTime = Instant.now()))
            }
        }
    }

    fun toggleSalaryVisibility() {
        _uiState.value = _uiState.value.copy(isSalaryVisible = !_uiState.value.isSalaryVisible)
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                _uiState.value.latestSession?.startTime?.let { startTime ->
                    val duration = Duration.between(startTime, Instant.now())
                    val hours = duration.toHours().toString().padStart(2, '0')
                    val minutes = (duration.toMinutes() % 60).toString().padStart(2, '0')
                    val seconds = (duration.seconds % 60).toString().padStart(2, '0')
                    _uiState.value = _uiState.value.copy(timer = "$hours:$minutes:$seconds")
                }
                delay(1000)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
    }
}
