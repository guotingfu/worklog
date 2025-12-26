package com.example.worklog.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worklog.data.local.db.Session
import com.example.worklog.data.local.db.SessionDao
import com.example.worklog.data.local.datastore.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

enum class Filter { WEEK, MONTH, QUARTER, YEAR }
enum class TimeUnit { HOURS, MINUTES }

data class WorkHours(
    val regular: Duration = Duration.ZERO,
    val overtime: Duration = Duration.ZERO
) {
    val total: Duration get() = regular + overtime
}

data class StatsUiState(
    val filter: Filter = Filter.WEEK,
    val timeUnit: TimeUnit = TimeUnit.HOURS,
    val chartData: Map<String, WorkHours> = emptyMap(),
    val totalWorkHours: WorkHours = WorkHours(),
    val realHourlyWage: Double = 0.0,
    val wageDiffPercentage: Double = 0.0,
    val overtimeSummary: String = ""
)

class StatsViewModel(
    private val sessionDao: SessionDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(Filter.WEEK)
    private val _timeUnit = MutableStateFlow(TimeUnit.HOURS)

    val uiState: StateFlow<StatsUiState> = combine(
        _filter, _timeUnit, sessionDao.getAllSessions(),
        settingsRepository.annualSalaryFlow, settingsRepository.startTimeFlow, settingsRepository.endTimeFlow
    ) { filter, timeUnit, sessions, annualSalaryStr, standardStartStr, standardEndStr ->

        if (sessions.isEmpty()) {
            return@combine StatsUiState()
        }

        val standardStartTime = LocalTime.parse(standardStartStr)
        val standardEndTime = LocalTime.parse(standardEndStr)
        val annualSalary = annualSalaryStr.toDoubleOrNull() ?: 0.0

        val (periodStart, periodEnd) = getPeriod(filter)
        val periodSessions = sessions.filter { it.startTime.isAfter(periodStart) && it.startTime.isBefore(periodEnd) }

        val workHoursByDay = periodSessions
            .flatMap { splitSession(it, standardStartTime, standardEndTime) }
            .groupBy { it.startTime.atZone(ZoneId.systemDefault()).toLocalDate() }
            .mapValues { entry ->
                val regular = entry.value.sumOf { it.regular.toMillis() }
                val overtime = entry.value.sumOf { it.overtime.toMillis() }
                WorkHours(Duration.ofMillis(regular), Duration.ofMillis(overtime))
            }

        val totalWorkHours = WorkHours(
            regular = Duration.ofMillis(workHoursByDay.values.sumOf { it.regular.toMillis() }),
            overtime = Duration.ofMillis(workHoursByDay.values.sumOf { it.overtime.toMillis() })
        )

        val (realWage, diff) = calculateRealHourlyWage(filter, periodStart, periodEnd, totalWorkHours.total, annualSalary, standardStartTime, standardEndTime)

        StatsUiState(
            filter = filter,
            timeUnit = timeUnit,
            chartData = formatChartData(workHoursByDay, filter),
            totalWorkHours = totalWorkHours,
            realHourlyWage = realWage,
            wageDiffPercentage = diff,
            overtimeSummary = if (totalWorkHours.overtime > Duration.ZERO) "本周期额外工作了 ${totalWorkHours.overtime.toHours()} 小时" else ""
        )

    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    private fun splitSession(session: Session, standardStart: LocalTime, standardEnd: LocalTime): List<WorkHours> {
        if (session.endTime == null) return emptyList()
        val sessionDate = session.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
        val isWeekend = sessionDate.dayOfWeek == DayOfWeek.SATURDAY || sessionDate.dayOfWeek == DayOfWeek.SUNDAY

        if (isWeekend) {
            return listOf(WorkHours(overtime = Duration.between(session.startTime, session.endTime)))
        }

        val standardStartInstant = sessionDate.atTime(standardStart).atZone(ZoneId.systemDefault()).toInstant()
        val standardEndInstant = sessionDate.atTime(standardEnd).atZone(ZoneId.systemDefault()).toInstant()

        val regularStart = maxOf(session.startTime, standardStartInstant)
        val regularEnd = minOf(session.endTime, standardEndInstant)

        val regular = if (regularStart.isBefore(regularEnd)) Duration.between(regularStart, regularEnd) else Duration.ZERO
        val total = Duration.between(session.startTime, session.endTime)
        val overtime = total - regular

        return listOf(WorkHours(regular, overtime))
    }

    private fun calculateRealHourlyWage(
        filter: Filter,
        periodStart: Instant,
        periodEnd: Instant,
        actualHoursDuration: Duration,
        annualSalary: Double,
        standardStart: LocalTime,
        standardEnd: LocalTime
    ): Pair<Double, Double> {
        if (annualSalary == 0.0) return Pair(0.0, 0.0)

        val periodPay = when (filter) {
            Filter.WEEK -> annualSalary / 52
            Filter.MONTH -> annualSalary / 12
            Filter.QUARTER -> annualSalary / 4
            Filter.YEAR -> annualSalary
        }

        val workdays = countWorkdays(periodStart, periodEnd)
        val standardWorkdayHours = Duration.between(standardStart, standardEnd)
        val standardTotalHours = standardWorkdayHours.multipliedBy(workdays.toLong())
        val actualTotalHours = actualHoursDuration

        val effectiveHours = maxOf(standardTotalHours, actualTotalHours)
        if (effectiveHours.isZero) return Pair(0.0, 0.0)

        val standardHourlyWage = periodPay / standardTotalHours.toHours().toDouble()
        val realHourlyWage = periodPay / effectiveHours.toHours().toDouble()

        val diff = if (standardHourlyWage > 0) (realHourlyWage - standardHourlyWage) / standardHourlyWage * 100 else 0.0

        return Pair(realHourlyWage, diff)
    }

    private fun countWorkdays(start: Instant, end: Instant): Int {
        var count = 0
        var currentDate = start.atZone(ZoneId.systemDefault()).toLocalDate()
        val endDate = end.atZone(ZoneId.systemDefault()).toLocalDate()
        while (!currentDate.isAfter(endDate)) {
            val day = currentDate.dayOfWeek
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                count++
            }
            currentDate = currentDate.plusDays(1)
        }
        return count
    }
    
    private fun getPeriod(filter: Filter): Pair<Instant, Instant> {
        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()
        return when (filter) {
            Filter.WEEK -> {
                val firstDayOfWeek = today.with(TemporalAdjusters.previousOrSame(WeekFields.of(Locale.getDefault()).firstDayOfWeek))
                Pair(firstDayOfWeek.atStartOfDay(zoneId).toInstant(), today.atTime(23, 59, 59).atZone(zoneId).toInstant())
            }
            Filter.MONTH -> Pair(today.withDayOfMonth(1).atStartOfDay(zoneId).toInstant(), today.atTime(23, 59, 59).atZone(zoneId).toInstant())
            Filter.QUARTER -> {
                val quarterStartMonth = ((today.monthValue - 1) / 3) * 3 + 1
                val quarterStart = LocalDate.of(today.year, quarterStartMonth, 1)
                Pair(quarterStart.atStartOfDay(zoneId).toInstant(), today.atTime(23, 59, 59).atZone(zoneId).toInstant())
            }
            Filter.YEAR -> Pair(today.withDayOfYear(1).atStartOfDay(zoneId).toInstant(), today.atTime(23, 59, 59).atZone(zoneId).toInstant())
        }
    }

    private fun formatChartData(data: Map<LocalDate, WorkHours>, filter: Filter): Map<String, WorkHours> {
        return when (filter) {
            Filter.WEEK -> data.mapKeys { it.key.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()) }
            Filter.MONTH -> data.mapKeys { it.key.dayOfMonth.toString() }
            Filter.QUARTER -> data.mapKeys { "Q${(it.key.monthValue -1) / 3 + 1} ${it.key.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())}" }
            Filter.YEAR -> data.mapKeys { it.key.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()) }
        }
    }

    fun setFilter(filter: Filter) {
        _filter.value = filter
    }

    fun setTimeUnit(timeUnit: TimeUnit) {
        _timeUnit.value = timeUnit
    }

    private fun <T : Comparable<T>> maxOf(a: T, b: T): T = if (a > b) a else b
    private fun <T : Comparable<T>> minOf(a: T, b: T): T = if (a < b) a else b
}
