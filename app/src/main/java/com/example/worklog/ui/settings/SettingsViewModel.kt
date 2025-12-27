package com.example.worklog.ui.settings

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worklog.data.local.db.Session
import com.example.worklog.data.local.db.SessionDao
import com.example.worklog.data.local.datastore.SettingsRepository
import com.example.worklog.data.local.datastore.Theme
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

class InstantAdapter : com.google.gson.JsonSerializer<Instant>, com.google.gson.JsonDeserializer<Instant> {
    override fun serialize(src: Instant?, typeOfSrc: java.lang.reflect.Type?, context: com.google.gson.JsonSerializationContext?): com.google.gson.JsonElement {
        return com.google.gson.JsonPrimitive(src?.toString())
    }
    override fun deserialize(json: com.google.gson.JsonElement?, typeOfT: java.lang.reflect.Type?, context: com.google.gson.JsonDeserializationContext?): Instant {
        return Instant.parse(json?.asString)
    }
}

data class SettingsUiState(
    val annualSalary: String = "",
    val theme: Theme = Theme.SYSTEM,
    val startTime: LocalTime = LocalTime.of(9, 0),
    val endTime: LocalTime = LocalTime.of(18, 0),
    val recentSessions: List<Session> = emptyList(),
    val workingDays: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
    // [新增] 提醒开关状态
    val reminderEnabled: Boolean = false
)

class SettingsViewModel(
    private val sessionDao: SessionDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val allSessionsFlow = sessionDao.getRecentSessions(50)

    // 组合所有数据源生成 UI 状态
    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.annualSalaryFlow,    // args[0]
        settingsRepository.themeFlow,           // args[1]
        settingsRepository.startTimeFlow,       // args[2]
        settingsRepository.endTimeFlow,         // args[3]
        allSessionsFlow,                        // args[4]
        settingsRepository.workingDaysFlow,     // args[5]
        settingsRepository.reminderEnabledFlow  // args[6] [新增]
    ) { args: Array<Any?> ->
        val salary = args[0] as String
        val theme = args[1] as Theme
        val startTimeStr = args[2] as String
        val endTimeStr = args[3] as String
        @Suppress("UNCHECKED_CAST")
        val sessions = args[4] as List<Session>
        @Suppress("UNCHECKED_CAST")
        val workingDays = args[5] as Set<DayOfWeek>
        val reminderEnabled = args[6] as Boolean

        val startTime = runCatching { LocalTime.parse(startTimeStr) }.getOrDefault(LocalTime.of(9, 0))
        val endTime = runCatching { LocalTime.parse(endTimeStr) }.getOrDefault(LocalTime.of(18, 0))

        SettingsUiState(
            annualSalary = salary,
            theme = theme,
            startTime = startTime,
            endTime = endTime,
            recentSessions = sessions.take(1), // 设置页只显示最近一条预览
            workingDays = workingDays,
            reminderEnabled = reminderEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    // 用于“所有记录”页面的完整数据流
    val allSessions: StateFlow<List<Session>> = allSessionsFlow.map { it.take(30) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val gson = GsonBuilder()
        .registerTypeAdapter(Instant::class.java, InstantAdapter())
        .create()

    fun updateAnnualSalary(salary: String) {
        viewModelScope.launch { settingsRepository.updateAnnualSalary(salary) }
    }

    fun updateTheme(theme: Theme) {
        viewModelScope.launch { settingsRepository.updateTheme(theme) }
    }

    fun updateStartTime(time: LocalTime) {
        viewModelScope.launch { settingsRepository.updateStartTime(time.toString()) }
    }

    fun updateEndTime(time: LocalTime) {
        viewModelScope.launch { settingsRepository.updateEndTime(time.toString()) }
    }

    fun updateWorkingDays(days: Set<DayOfWeek>) {
        viewModelScope.launch { settingsRepository.updateWorkingDays(days) }
    }

    // [新增] 更新提醒开关
    fun updateReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateReminderEnabled(enabled) }
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch { sessionDao.deleteSessionById(id) }
    }

    fun updateSession(session: Session) {
        viewModelScope.launch { sessionDao.update(session) }
    }

    fun backupData(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sessions = sessionDao.getAllSessionsList()
                val json = gson.toJson(sessions)
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(json.toByteArray())
                }
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "备份成功", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "备份失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun importData(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val stringBuilder = StringBuilder()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            stringBuilder.append(line)
                            line = reader.readLine()
                        }
                    }
                }
                val json = stringBuilder.toString()
                val type = object : TypeToken<List<Session>>() {}.type
                val sessions: List<Session> = gson.fromJson(json, type)

                val threshold = Instant.parse("2020-01-01T00:00:00Z")
                val validSessions = sessions.filter { it.startTime.isAfter(threshold) }
                sessionDao.insertAll(validSessions)

                launch(Dispatchers.Main) {
                    Toast.makeText(context, "导入成功，有效记录 ${validSessions.size} 条", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "导入失败:文件格式错误", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}