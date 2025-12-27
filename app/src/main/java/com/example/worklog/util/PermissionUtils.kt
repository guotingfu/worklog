package com.example.worklog.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import java.util.Locale

object PermissionUtils {

    // 检查是否在电池优化白名单中 (true = 已无视电池优化/白名单中)
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }
        return true
    }

    // 跳转电池优化设置
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 跳转通知设置
    fun openNotificationSettings(context: Context) {
        val intent = Intent().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            } else {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", context.packageName, null)
            }
        }
        context.startActivity(intent)
    }

    // 跳转自启动管理页面 (适配主流国产机型)
    fun openAutoStartSettings(context: Context) {
        val intent = Intent()
        try {
            val manufacturer = Build.MANUFACTURER.lowercase(Locale.getDefault())
            when {
                manufacturer.contains("xiaomi") -> {
                    intent.component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                }
                manufacturer.contains("oppo") -> {
                    intent.component = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
                }
                manufacturer.contains("vivo") -> {
                    intent.component = ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
                }
                manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                    intent.component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
                }
                manufacturer.contains("samsung") -> {
                    intent.component = ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")
                }
                else -> {
                    // 其他机型跳转到应用详情页
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    intent.data = Uri.fromParts("package", context.packageName, null)
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // 如果特定 Intent 失败，回退到应用详情
            val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            fallbackIntent.data = Uri.fromParts("package", context.packageName, null)
            context.startActivity(fallbackIntent)
        }
    }
}