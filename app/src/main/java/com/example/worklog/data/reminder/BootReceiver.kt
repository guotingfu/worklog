package com.example.worklog.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.worklog.WorkLogApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class BootReceiver : BroadcastReceiver() {

    // 使用 SupervisorJob + IO 线程来处理数据库读取任务
    // 这样不会阻塞主线程，且容错性更强
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        // 只处理“开机完成”的广播
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "手机已重启，正在恢复打卡提醒闹钟...")

            // 获取应用的全局容器，以便访问 Repository
            val application = context.applicationContext as WorkLogApplication
            val settingsRepo = application.container.settingsRepository
            val scheduler = ReminderScheduler(context)

            // 开启协程读取 DataStore
            scope.launch {
                try {
                    // 1. 检查总开关：如果用户没开提醒，直接不处理
                    val reminderEnabled = settingsRepo.reminderEnabledFlow.first()
                    if (!reminderEnabled) {
                        Log.d("BootReceiver", "提醒功能未开启，跳过设置。")
                        return@launch
                    }

                    // 2. 读取用户设定的时间
                    val startTimeStr = settingsRepo.startTimeFlow.first()
                    val endTimeStr = settingsRepo.endTimeFlow.first()

                    val startTime = runCatching { LocalTime.parse(startTimeStr) }.getOrDefault(LocalTime.of(9, 0))
                    val endTime = runCatching { LocalTime.parse(endTimeStr) }.getOrDefault(LocalTime.of(18, 0))

                    // 3. 计算“今天”应该触发提醒的时间点
                    val todayDate = LocalDate.now()
                    val zoneId = ZoneId.systemDefault()
                    val nowInstant = LocalDateTime.now().atZone(zoneId).toInstant()

                    // 上班提醒时间 = 上班时间 + 30分钟
                    val startReminderInstant = todayDate.atTime(startTime).plusMinutes(30).atZone(zoneId).toInstant()

                    // 下班提醒时间 = 下班时间 + 60分钟
                    val endReminderInstant = todayDate.atTime(endTime).plusMinutes(60).atZone(zoneId).toInstant()

                    // 4. 只有当时间点在“未来”时，才重新设定闹钟
                    // (如果重启时已经是晚上10点，就没必要恢复早上9点30的闹钟了)

                    if (startReminderInstant.isAfter(nowInstant)) {
                        scheduler.scheduleReminder(startReminderInstant, 0) // type 0 = 上班
                        Log.d("BootReceiver", "已恢复上班提醒: $startReminderInstant")
                    }

                    if (endReminderInstant.isAfter(nowInstant)) {
                        scheduler.scheduleReminder(endReminderInstant, 1) // type 1 = 下班
                        Log.d("BootReceiver", "已恢复下班提醒: $endReminderInstant")
                    }

                } catch (e: Exception) {
                    Log.e("BootReceiver", "恢复闹钟失败", e)
                }
            }
        }
    }
}