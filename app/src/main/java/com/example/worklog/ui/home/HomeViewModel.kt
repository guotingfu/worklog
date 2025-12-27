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
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate

data class HomeUiState(
    val isWorking: Boolean = false,
    val timer: String = "00:00:00",
    val annualSalary: String = "",
    val isSalaryVisible: Boolean = false,
    val latestSession: Session? = null,
    // [新增] 用于UI显示的节假日状态，它是 DB状态 和 用户临时操作 的结合结果
    val uiIsHoliday: Boolean = false
)

class HomeViewModel(
    private val sessionDao: SessionDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    // [新增] 暂存用户在“未打卡”状态下的勾选意图
    // null = 无操作，跟随数据库或默认值
    private var userSelectedHoliday: Boolean? = null

    init {
        combine(
            sessionDao.getLatestSession(),
            settingsRepository.annualSalaryFlow
        ) { session, salary ->
            // 判断 latestSession 是否是“今天”的记录
            val isSessionToday = session != null && isSameDay(session.startTime, Instant.now())

            // 计算当前的节假日状态：
            // 1. 如果用户刚刚手动操作过(userSelectedHoliday != null)，以用户操作为准
            // 2. 否则，如果 session 是今天的，以 session 状态为准
            // 3. 否则（session是昨天的或没有），默认为 false
            val currentHolidayStatus = userSelectedHoliday ?: (isSessionToday && session?.isHoliday == true)

            _uiState.value = _uiState.value.copy(
                latestSession = session,
                isWorking = session != null && session.endTime == null,
                annualSalary = salary,
                uiIsHoliday = currentHolidayStatus
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
            val currentState = _uiState.value

            if (currentState.isWorking) {
                // 下班
                currentState.latestSession?.let {
                    sessionDao.update(it.copy(endTime = Instant.now()))
                }
                // 下班后，重置用户的临时选择，之后的逻辑完全依赖 DB 记录
                userSelectedHoliday = null
            } else {
                // 上班
                // 确定新 session 的节假日状态
                val isHoliday = currentState.uiIsHoliday

                sessionDao.insert(Session(
                    startTime = Instant.now(),
                    endTime = null,
                    isHoliday = isHoliday // 写入状态
                ))
                // 上班后，同样重置临时选择
                userSelectedHoliday = null
            }
        }
    }

    // [修改] 切换节假日逻辑
    fun toggleHoliday(targetState: Boolean) {
        // 更新本地临时状态，让 UI 立即响应（无论是否在打卡中）
        userSelectedHoliday = targetState

        // 触发 UI 刷新 (通过重新发射 combine 需要一点技巧，这里直接更新 StateFlow 会被 combine 覆盖，
        // 但由于 userSelectedHoliday 变了，combine 下一次运行时会取到新值。
        // 为了即时性，我们手动更新数据库（如果在打卡中或有今天的记录）)

        viewModelScope.launch {
            val session = _uiState.value.latestSession
            val isSessionToday = session != null && isSameDay(session.startTime, Instant.now())

            // 如果当前正在工作，或者最近一条记录是今天的，则立即同步到数据库
            // 这样能保证“当天无论打卡几次，状态保持不变”（因为下一条记录会继承上一条）
            if (isSessionToday) {
                sessionDao.update(session!!.copy(isHoliday = targetState))
                // 写入 DB 后，userSelectedHoliday 可以置空，依靠 DB 驱动 UI
                userSelectedHoliday = null
            } else {
                // 如果没有今天的记录（比如还没上班），只更新 userSelectedHoliday
                // 强制触发一次 UI 更新
                _uiState.value = _uiState.value.copy(uiIsHoliday = targetState)
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

    // 辅助方法：判断是否同一天
    private fun isSameDay(instant1: Instant, instant2: Instant): Boolean {
        val zone = ZoneId.systemDefault()
        val date1 = instant1.atZone(zone).toLocalDate()
        val date2 = instant2.atZone(zone).toLocalDate()
        return date1 == date2
    }
}