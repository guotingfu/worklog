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
import java.time.temporal.WeekFields
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
    val isLoading: Boolean = true,
    val standardWorkHours: Double = 9.0 // [新增] 用于UI判断是否显示异常提示
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
        settingsRepository.endTimeFlow
    ) { args: Array<Any?> ->
        val period = args[0] as StatsPeriod
        val unit = args[1] as TimeDisplayUnit
        val sessions = args[2] as List<Session>
        val salaryStr = args[3] as String
        val startStr = args[4] as String
        val endStr = args[5] as String

        val standardWorkStartTime = runCatching { LocalTime.parse(startStr) }.getOrDefault(LocalTime.of(9, 0))
        val standardWorkEndTime = runCatching { LocalTime.parse(endStr) }.getOrDefault(LocalTime.of(18, 0))
        val annualSalary = salaryStr.toDoubleOrNull() ?: 0.0
        val standardWorkDayDuration = Duration.between(standardWorkStartTime, standardWorkEndTime)

        // [修改] 计算标准工作小时数，传给UI用于判断异常
        val standardWorkHours = standardWorkDayDuration.toMillis() / 3600_000.0

        val (periodStart, periodEnd) = getPeriod(period)
        val periodSessions = sessions.filter { it.startTime.isAfter(periodStart) && it.startTime.isBefore(periodEnd) }

        val chartData = processChartData(periodSessions, period, standardWorkDayDuration, unit)

        val totalWorkDuration = periodSessions.sumOf { Duration.between(it.startTime, it.endTime ?: it.startTime).toMillis() }
        val overtimeDuration = calculateOvertime(periodSessions, standardWorkStartTime, standardWorkEndTime)

        val (actualWage, isStandard) = calculateActualWage(
            annualSalary,
            totalWorkDuration,
            standardWorkDayDuration,
            countWorkdays(periodStart, periodEnd),
            unit
        )

        val divisor = if (unit == TimeDisplayUnit.HOUR) 3600_000.0 else 60_000.0

        StatsUiState(
            selectedPeriod = period,
            selectedUnit = unit,
            chartData = chartData,
            totalWorkDuration = totalWorkDuration / divisor,
            overtimeDuration = overtimeDuration / divisor,
            actualWage = actualWage,
            isWageStandard = isStandard,
            isLoading = false,
            standardWorkHours = standardWorkHours // [新增]
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    fun setPeriod(period: StatsPeriod) { _selectedPeriod.value = period }
    fun setUnit(unit: TimeDisplayUnit) { _selectedUnit.value = unit }

    private fun getPeriod(period: StatsPeriod): Pair<Instant, Instant> {
        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()
        return when (period) {
            StatsPeriod.WEEK -> {
                val firstDayOfWeek = today.with(WeekFields.of(Locale.CHINESE).firstDayOfWeek)
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

        when (period) {
            StatsPeriod.WEEK -> {
                val firstDay = LocalDate.now().with(WeekFields.of(Locale.CHINESE).firstDayOfWeek)
                (0..6).forEach { i ->
                    val day = firstDay.plusDays(i.toLong())
                    val daySessions = sessions.filter { it.startTime.atZone(zoneId).toLocalDate() == day }
                    val totalMillis = daySessions.sumOf { Duration.between(it.startTime, it.endTime ?: it.startTime).toMillis() }
                    val value = totalMillis / divisor
                    if (value > standardDuration.toMillis() / divisor) {
                        overEntries.add(FloatEntry(i.toFloat(), value))
                        underEntries.add(FloatEntry(i.toFloat(), 0f))
                    } else {
                        underEntries.add(FloatEntry(i.toFloat(), value))
                        overEntries.add(FloatEntry(i.toFloat(), 0f))
                    }
                    labels.add(day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINESE))
                }
            }
            StatsPeriod.MONTH -> {
                val month = YearMonth.now()
                (1..month.lengthOfMonth()).forEach { i ->
                    val day = month.atDay(i)
                    val daySessions = sessions.filter { it.startTime.atZone(zoneId).toLocalDate() == day }
                    val totalMillis = daySessions.sumOf { Duration.between(it.startTime, it.endTime ?: it.startTime).toMillis() }
                    val value = totalMillis / divisor
                    if (value > standardDuration.toMillis() / divisor) {
                        overEntries.add(FloatEntry(i.toFloat() -1 , value))
                        underEntries.add(FloatEntry(i.toFloat() -1, 0f))
                    } else {
                        underEntries.add(FloatEntry(i.toFloat() - 1, value))
                        overEntries.add(FloatEntry(i.toFloat() - 1, 0f))
                    }
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
                    val value = totalMillis / divisor
                    if (value > standardMonthlyMillis / divisor) {
                        overEntries.add(FloatEntry(i.toFloat() - 1, value))
                        underEntries.add(FloatEntry(i.toFloat() - 1, 0f))
                    } else {
                        underEntries.add(FloatEntry(i.toFloat() - 1, value))
                        overEntries.add(FloatEntry(i.toFloat() - 1, 0f))
                    }
                    labels.add(Month.of(i).getDisplayName(TextStyle.SHORT, Locale.CHINESE))
                }
            }
        }
        return ChartData(listOf(overEntries, underEntries), labels)
    }

    private fun calculateOvertime(sessions: List<Session>, standardStart: LocalTime, standardEnd: LocalTime): Double {
        return sessions.sumOf { session ->
            if (session.endTime == null) return@sumOf 0.0
            val sessionDate = session.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
            val standardStartInstant = sessionDate.atTime(standardStart).atZone(ZoneId.systemDefault()).toInstant()
            val standardEndInstant = sessionDate.atTime(standardEnd).atZone(ZoneId.systemDefault()).toInstant()

            val beforeStart = if (session.startTime.isBefore(standardStartInstant)) Duration.between(session.startTime, standardStartInstant).toMillis() else 0
            val afterEnd = if (session.endTime.isAfter(standardEndInstant)) Duration.between(standardEndInstant, session.endTime).toMillis() else 0

            (beforeStart + afterEnd).toDouble()
        }
    }

    private fun calculateActualWage(annualSalary: Double, totalWorkMillis: Long, standardDayDuration: Duration, workdays: Int, unit: TimeDisplayUnit): Pair<Double, Boolean> {
        if (annualSalary == 0.0) return Pair(0.0, true)

        val workingDaysInYear = 251.0
        val standardWorkHoursInYear = workingDaysInYear * (standardDayDuration.toMillis() / 3600_000.0)
        if (standardWorkHoursInYear == 0.0) return Pair(0.0, true)

        val standardWage = if (unit == TimeDisplayUnit.HOUR) annualSalary / standardWorkHoursInYear else annualSalary / (standardWorkHoursInYear * 60)

        val standardTotalDuration = standardDayDuration.multipliedBy(workdays.toLong())
        val effectiveDuration = maxOf(Duration.ofMillis(totalWorkMillis), standardTotalDuration)

        val effectiveTotalHours = effectiveDuration.toMillis() / 3600_000.0
        if (effectiveTotalHours == 0.0) return Pair(standardWage, true)

        val periodPay = annualSalary * (workdays / workingDaysInYear)
        val actualWage = if (unit == TimeDisplayUnit.HOUR) periodPay / effectiveTotalHours else periodPay / (effectiveTotalHours * 60)

        return Pair(actualWage, actualWage >= standardWage)
    }

    private fun countWorkdays(start: Instant, end: Instant): Int {
        var count = 0
        val zoneId = ZoneId.systemDefault()
        var currentDate = start.atZone(zoneId).toLocalDate()
        val endDate = end.atZone(zoneId).toLocalDate()
        while (currentDate.isBefore(endDate)) {
            if (currentDate.dayOfWeek !in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
                count++
            }
            currentDate = currentDate.plusDays(1)
        }
        return count
    }

    private fun maxOf(a: Duration, b: Duration): Duration = if (a > b) a else b
}