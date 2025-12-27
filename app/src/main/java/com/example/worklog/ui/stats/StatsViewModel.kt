package com.example.worklog.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worklog.data.local.db.Session
import com.example.worklog.data.local.db.SessionDao
import com.example.worklog.data.local.datastore.SettingsRepository
import com.patrykandpatrick.vico.core.entry.ChartEntry
import com.patrykandpatrick.vico.core.entry.FloatEntry
import kotlinx.coroutines.flow.*
import java.time.*
import java.time.format.TextStyle
import java.util.Locale

enum class StatsPeriod { WEEK, MONTH, YEAR }
enum class TimeDisplayUnit { HOUR, MINUTE }

data class ChartData(
    val entries: List<List<ChartEntry>> = emptyList(),
    val labels: List<String> = emptyList()
)

data class StatsUiState(
    val selectedPeriod: StatsPeriod = StatsPeriod.WEEK,
    val selectedUnit: TimeDisplayUnit = TimeDisplayUnit.HOUR,
    val chartData: ChartData = ChartData(),
    val totalWorkDuration: Double = 0.0,
    val overtimeDuration: Double = 0.0,
    val actualWage: Double = 0.0,
    val isWageStandard: Boolean = true,
    val wageDecreasePercent: Double = 0.0,
    val isLoading: Boolean = true,
    val standardWorkHours: Double = 9.0,
    val errorMessage: String? = null
)

class StatsViewModel(
    private val sessionDao: SessionDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(StatsPeriod.WEEK)
    private val _selectedUnit = MutableStateFlow(TimeDisplayUnit.HOUR)

    val uiState: StateFlow<StatsUiState> = combine(
        _selectedPeriod,
        _selectedUnit,
        sessionDao.getAllSessions(),
        settingsRepository.annualSalaryFlow,
        settingsRepository.startTimeFlow,
        settingsRepository.endTimeFlow,
        settingsRepository.workingDaysFlow
    ) { args: Array<Any?> ->
        try {
            val period = args[0] as? StatsPeriod ?: StatsPeriod.WEEK
            val unit = args[1] as? TimeDisplayUnit ?: TimeDisplayUnit.HOUR
            @Suppress("UNCHECKED_CAST")
            val sessions = args[2] as? List<Session> ?: emptyList()
            val salaryStr = args[3] as? String ?: ""
            val startStr = args[4] as? String ?: ""
            val endStr = args[5] as? String ?: ""
            @Suppress("UNCHECKED_CAST")
            val userWorkingDays = args[6] as? Set<DayOfWeek>
                ?: setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)

            val standardWorkStartTime = runCatching { LocalTime.parse(startStr) }.getOrDefault(LocalTime.of(9, 0))
            val standardWorkEndTime = runCatching { LocalTime.parse(endStr) }.getOrDefault(LocalTime.of(18, 0))
            val annualSalary = salaryStr.toDoubleOrNull() ?: 0.0

            val rawDuration = Duration.between(standardWorkStartTime, standardWorkEndTime)
            val standardWorkDayDuration = if (rawDuration.isNegative) rawDuration.plusDays(1) else rawDuration
            val standardWorkHours = standardWorkDayDuration.toMillis() / 3600_000.0

            val (periodStart, periodEnd) = getPeriod(period)

            // [修复] 修正筛选逻辑，使用 >=（即 !isBefore）来确保第一天的数据被包含
            val periodSessions = sessions.filter {
                !it.startTime.isBefore(periodStart) && it.startTime.isBefore(periodEnd)
            }

            val chartData = processChartData(periodSessions, period, standardWorkDayDuration, unit)

            val totalWorkDurationMillis = periodSessions.sumOf {
                val end = it.endTime ?: it.startTime
                if (end.isBefore(it.startTime)) 0L else Duration.between(it.startTime, end).toMillis()
            }

            val overtimeDurationMillis = calculateOvertime(periodSessions, standardWorkStartTime, standardWorkEndTime, userWorkingDays)

            val (actualWage, isStandard, decreasePercent) = calculateActualWage(
                annualSalary,
                standardWorkDayDuration,
                countStandardWorkdaysInPeriod(periodStart, periodEnd, userWorkingDays),
                unit,
                userWorkingDays.size,
                overtimeDurationMillis
            )

            val divisor = if (unit == TimeDisplayUnit.HOUR) 3600_000.0 else 60_000.0

            StatsUiState(
                selectedPeriod = period,
                selectedUnit = unit,
                chartData = chartData,
                totalWorkDuration = totalWorkDurationMillis / divisor,
                overtimeDuration = overtimeDurationMillis / divisor,
                actualWage = actualWage,
                isWageStandard = isStandard,
                wageDecreasePercent = decreasePercent,
                isLoading = false,
                standardWorkHours = standardWorkHours,
                errorMessage = null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            StatsUiState(errorMessage = "数据处理异常: ${e.javaClass.simpleName} - ${e.message}")
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    fun setPeriod(period: StatsPeriod) { _selectedPeriod.value = period }
    fun setUnit(unit: TimeDisplayUnit) { _selectedUnit.value = unit }

    private fun getPeriod(period: StatsPeriod): Pair<Instant, Instant> {
        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()
        return when (period) {
            StatsPeriod.WEEK -> {
                // [修复] 使用 DayOfWeek.MONDAY 保证周一为一周的开始，更稳定
                val firstDayOfWeek = today.with(DayOfWeek.MONDAY)
                Pair(firstDayOfWeek.atStartOfDay(zoneId).toInstant(), firstDayOfWeek.plusDays(7).atStartOfDay(zoneId).toInstant())
            }
            StatsPeriod.MONTH -> {
                val firstDayOfMonth = today.withDayOfMonth(1)
                Pair(firstDayOfMonth.atStartOfDay(zoneId).toInstant(), firstDayOfMonth.plusMonths(1).atStartOfDay(zoneId).toInstant())
            }
            StatsPeriod.YEAR -> {
                val firstDayOfYear = today.withDayOfYear(1)
                Pair(firstDayOfYear.atStartOfDay(zoneId).toInstant(), firstDayOfYear.plusYears(1).atStartOfDay(zoneId).toInstant())
            }
        }
    }

    private fun processChartData(sessions: List<Session>, period: StatsPeriod, standardDuration: Duration, unit: TimeDisplayUnit): ChartData {
        val overEntries = mutableListOf<FloatEntry>()
        val underEntries = mutableListOf<FloatEntry>()
        val labels = mutableListOf<String>()
        val divisor = if (unit == TimeDisplayUnit.HOUR) 3600_000f else 60_000f
        val zoneId = ZoneId.systemDefault()

        fun addEntry(x: Float, totalMillis: Long, standardMillis: Long) {
            val totalVal = totalMillis / divisor
            val standardVal = standardMillis / divisor

            if (totalVal > standardVal) {
                underEntries.add(FloatEntry(x, standardVal))
                overEntries.add(FloatEntry(x, totalVal - standardVal))
            } else {
                underEntries.add(FloatEntry(x, totalVal))
                overEntries.add(FloatEntry(x, 0f))
            }
        }

        when (period) {
            StatsPeriod.WEEK -> {
                // [修复] 使用 DayOfWeek.MONDAY 保证与 getPeriod 逻辑一致
                val firstDay = LocalDate.now().with(DayOfWeek.MONDAY)
                (0..6).forEach { i ->
                    val day = firstDay.plusDays(i.toLong())
                    val daySessions = sessions.filter { it.startTime.atZone(zoneId).toLocalDate() == day }
                    val totalMillis = daySessions.sumOf { Duration.between(it.startTime, it.endTime ?: it.startTime).toMillis() }

                    addEntry(i.toFloat(), totalMillis, standardDuration.toMillis())
                    labels.add(day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINESE))
                }
            }
            StatsPeriod.MONTH -> {
                val month = YearMonth.now()
                (1..month.lengthOfMonth()).forEach { i ->
                    val day = month.atDay(i)
                    val daySessions = sessions.filter { it.startTime.atZone(zoneId).toLocalDate() == day }
                    val totalMillis = daySessions.sumOf { Duration.between(it.startTime, it.endTime ?: it.startTime).toMillis() }

                    addEntry(i.toFloat() - 1, totalMillis, standardDuration.toMillis())
                    labels.add(i.toString())
                }
            }
            StatsPeriod.YEAR -> {
                (1..12).forEach { i ->
                    val monthSessions = sessions.filter { it.startTime.atZone(zoneId).monthValue == i }
                    val totalMillis = monthSessions.sumOf { Duration.between(it.startTime, it.endTime ?: it.startTime).toMillis() }
                    val daysInMonth = YearMonth.of(LocalDate.now().year, i).lengthOfMonth()
                    val workdaysInMonth = (1..daysInMonth).count { day ->
                        val date = LocalDate.of(LocalDate.now().year, i, day)
                        date.dayOfWeek !in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                    }
                    val standardMonthlyMillis = standardDuration.toMillis() * workdaysInMonth

                    addEntry(i.toFloat() - 1, totalMillis, standardMonthlyMillis)
                    labels.add(Month.of(i).getDisplayName(TextStyle.SHORT, Locale.CHINESE))
                }
            }
        }
        // [修复] 交换顺序，让 underEntries (绿色) 在底部，overEntries (红色) 在顶部
        return ChartData(listOf(underEntries, overEntries), labels)
    }

    private fun calculateOvertime(
        sessions: List<Session>,
        standardStart: LocalTime,
        standardEnd: LocalTime,
        workingDays: Set<DayOfWeek>
    ): Double {
        return sessions.sumOf { session ->
            if (session.endTime == null) return@sumOf 0.0

            val zoneId = ZoneId.systemDefault()
            val sessionDate = session.startTime.atZone(zoneId).toLocalDate()
            val isWorkDay = sessionDate.dayOfWeek in workingDays && !session.isHoliday

            if (!isWorkDay) {
                Duration.between(session.startTime, session.endTime).toMillis().toDouble()
            } else {
                val standardStartInstant = sessionDate.atTime(standardStart).atZone(zoneId).toInstant()
                val standardEndInstant = sessionDate.atTime(standardEnd).atZone(zoneId).toInstant()

                val beforeStart = if (session.startTime.isBefore(standardStartInstant)) Duration.between(session.startTime, standardStartInstant).toMillis() else 0

                val effectiveStandardEnd = if (standardEnd.isBefore(standardStart)) standardEndInstant.plus(Duration.ofDays(1)) else standardEndInstant

                val afterEnd = if (session.endTime.isAfter(effectiveStandardEnd)) Duration.between(effectiveStandardEnd, session.endTime).toMillis() else 0

                (beforeStart + afterEnd).toDouble()
            }
        }
    }

    private fun calculateActualWage(
        annualSalary: Double,
        standardDayDuration: Duration,
        standardWorkDaysInPeriod: Int,
        unit: TimeDisplayUnit,
        daysPerWeek: Int,
        overtimeMillis: Double
    ): Triple<Double, Boolean, Double> {
        if (annualSalary == 0.0) return Triple(0.0, true, 0.0)

        val standardDaysInYear = (daysPerWeek * 52.0) - 11.0
        val safeStandardDaysInYear = if (standardDaysInYear <= 0) 1.0 else standardDaysInYear

        val standardWorkHoursInYear = safeStandardDaysInYear * (standardDayDuration.toMillis() / 3600_000.0)
        if (standardWorkHoursInYear == 0.0) return Triple(0.0, true, 0.0)

        val standardWage = if (unit == TimeDisplayUnit.HOUR) annualSalary / standardWorkHoursInYear else annualSalary / (standardWorkHoursInYear * 60)

        val standardTotalMillis = standardDayDuration.toMillis() * standardWorkDaysInPeriod
        val effectiveTotalMillis = standardTotalMillis + overtimeMillis

        val effectiveTotalHours = effectiveTotalMillis / 3600_000.0
        if (effectiveTotalHours == 0.0) return Triple(standardWage, true, 0.0)

        val periodPay = annualSalary * (standardWorkDaysInPeriod.toDouble() / safeStandardDaysInYear)
        val actualWage = if (unit == TimeDisplayUnit.HOUR) periodPay / effectiveTotalHours else periodPay / (effectiveTotalHours * 60)

        val isStandard = actualWage >= (standardWage - 0.01)
        val decreasePercent = if (isStandard) 0.0 else (standardWage - actualWage) / standardWage

        return Triple(actualWage, isStandard, decreasePercent)
    }

    private fun countStandardWorkdaysInPeriod(start: Instant, end: Instant, workingDays: Set<DayOfWeek>): Int {
        var count = 0
        val zoneId = ZoneId.systemDefault()
        var currentDate = start.atZone(zoneId).toLocalDate()
        val endDate = end.atZone(zoneId).toLocalDate()
        while (currentDate.isBefore(endDate)) {
            if (currentDate.dayOfWeek in workingDays) {
                count++
            }
            currentDate = currentDate.plusDays(1)
        }
        return count
    }
}