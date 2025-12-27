package com.example.worklog.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import java.time.Instant
import java.time.ZoneId

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // 设定上班提醒 (type = 0: 上班, 1: 下班)
    fun scheduleReminder(triggerTime: Instant, type: Int) {
        // Android 12+ 需要检查精确闹钟权限，这里简化处理，实际需引导用户去设置开启
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("TYPE", type) // 传参告诉接收器是上班还是下班
        }

        // PendingIntent 需要 FLAG_IMMUTABLE 或 MUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            type, // unique ID
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 设定精确闹钟，即使在 Doze 模式下也能唤醒
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime.toEpochMilli(),
            pendingIntent
        )
    }

    // 取消闹钟 (例如用户已经主动打卡了)
    fun cancelReminder(type: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            type,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}