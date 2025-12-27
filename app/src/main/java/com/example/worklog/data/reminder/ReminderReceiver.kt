package com.example.worklog.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.worklog.MainActivity
import com.example.worklog.R
import com.example.worklog.WorkLogApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

class ReminderReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getIntExtra("TYPE", 0)

        val pendingResult = goAsync()

        scope.launch {
            try {
                val app = context.applicationContext as WorkLogApplication
                // [重点] 这里现在应该能正确引用到 sessionDao 了
                val sessionDao = app.container.sessionDao
                val settingsRepo = app.container.settingsRepository

                val workingDays = settingsRepo.workingDaysFlow.first()
                val today = Instant.now().atZone(ZoneId.systemDefault()).dayOfWeek
                if (today !in workingDays) {
                    return@launch
                }

                val latestSession = sessionDao.getLatestSession().first()
                val isTodayRecord = latestSession != null &&
                        latestSession.startTime.atZone(ZoneId.systemDefault()).toLocalDate() ==
                        Instant.now().atZone(ZoneId.systemDefault()).toLocalDate()

                val shouldNotify = if (type == 0) {
                    !isTodayRecord
                } else {
                    isTodayRecord && latestSession?.endTime == null
                }

                if (shouldNotify) {
                    showNotification(context, type)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, type: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "attendance_channel",
                "打卡提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "上下班打卡提醒"
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val title = if (type == 0) "上班打卡提醒" else "下班打卡提醒"
        val content = if (type == 0) "已过上班时间30分钟，您似乎忘记打卡了？" else "已过下班时间60分钟，还没走吗？"

        val activityIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, activityIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, "attendance_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(type, builder.build())
        }
    }
}