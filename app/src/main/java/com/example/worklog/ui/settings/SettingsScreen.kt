package com.example.worklog.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.worklog.data.local.db.Session
import com.example.worklog.data.local.datastore.Theme
import com.example.worklog.ui.theme.*
import com.example.worklog.util.PermissionUtils
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onUpdateAnnualSalary: (TextFieldValue) -> Unit,
    onUpdateTheme: (Theme) -> Unit,
    onUpdateStartTime: (LocalTime) -> Unit,
    onUpdateEndTime: (LocalTime) -> Unit,
    onUpdateWorkingDays: (Set<DayOfWeek>) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onUpdateSession: (Session) -> Unit,
    onBackupData: (Uri) -> Unit,
    onImportData: (Uri) -> Unit,
    // [新增] 切换提醒开关的回调，默认为空以兼容旧代码，但Navigation必须传
    onUpdateReminderEnabled: (Boolean) -> Unit = {},
    onNavigateToAllSessions: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 管理弹窗显示
    var showPermissionDialog by remember { mutableStateOf(false) }

    // 实时检测权限状态
    var hasNotificationPerm by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    var isBatteryWhitelisted by remember {
        mutableStateOf(PermissionUtils.isIgnoringBatteryOptimizations(context))
    }

    // 监听生命周期，当用户从设置页面返回 App 时刷新权限状态
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasNotificationPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                }
                isBatteryWhitelisted = PermissionUtils.isIgnoringBatteryOptimizations(context)

                // 如果所有硬性权限都满足，且本来是要开启提醒的，则自动更新状态
                if (showPermissionDialog && hasNotificationPerm && isBatteryWhitelisted) {
                    // 可选：自动关闭弹窗并开启开关，这里保留手动关闭以确认
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri -> if (uri != null) onBackupData(uri) }
    )
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> if (uri != null) onImportData(uri) }
    )

    var isAnnualSalaryFocused by remember { mutableStateOf(false) }
    var annualSalary by remember { mutableStateOf(TextFieldValue(uiState.annualSalary)) }

    LaunchedEffect(uiState.annualSalary) {
        if (!isAnnualSalaryFocused && annualSalary.text != uiState.annualSalary) {
            val newText = uiState.annualSalary
            annualSalary = TextFieldValue(newText, TextRange(newText.length))
        }
    }

    val salaryNum = annualSalary.text.toDoubleOrNull() ?: 0.0
    val salarySuffix = if (salaryNum >= 10000) {
        val isExact = salaryNum % 1000 == 0.0
        val prefix = if (isExact) "=" else "≈"
        "$prefix${String.format("%.1f", salaryNum / 10000)}万"
    } else null

    // 弹窗逻辑
    if (showPermissionDialog) {
        PermissionGuideDialog(
            hasNotificationPerm = hasNotificationPerm,
            isBatteryWhitelisted = isBatteryWhitelisted,
            onDismiss = { showPermissionDialog = false },
            onAllGranted = {
                onUpdateReminderEnabled(true)
                showPermissionDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SectionTitle("工作配置") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = annualSalary,
                        onValueChange = { newValue -> if (newValue.text.all { it.isDigit() }) { annualSalary = newValue; onUpdateAnnualSalary(newValue) } },
                        label = { Text("年薪 (元)") }, leadingIcon = { Text("¥") },
                        suffix = salarySuffix?.let { { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary) } },
                        modifier = Modifier.fillMaxWidth().onFocusChanged { isAnnualSalaryFocused = it.isFocused },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimePickerItem(label = "上班时间", time = uiState.startTime, onTimeSelected = onUpdateStartTime, modifier = Modifier.weight(1f))
                        TimePickerItem(label = "下班时间", time = uiState.endTime, onTimeSelected = onUpdateEndTime, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("工作日设定 (蓝色为工作日)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(8.dp))
                    WorkingDaysSelector(
                        selectedDays = uiState.workingDays,
                        onSelectionChanged = onUpdateWorkingDays
                    )
                }
            }
        }

        // [新增] 提醒功能 Section
        item { SectionTitle("提醒设置") }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!uiState.reminderEnabled) {
                                // 尝试开启：检查权限
                                if (!hasNotificationPerm || !isBatteryWhitelisted) {
                                    showPermissionDialog = true
                                } else {
                                    // 权限都有了，直接开
                                    onUpdateReminderEnabled(true)
                                }
                            } else {
                                // 关闭无需检查
                                onUpdateReminderEnabled(false)
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("忘打卡提醒", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.reminderEnabled) "已开启忘打卡提醒功能" else "迟到30分钟 / 下班60分钟后提醒",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.reminderEnabled) SuccessGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = uiState.reminderEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (!hasNotificationPerm || !isBatteryWhitelisted) {
                                    showPermissionDialog = true
                                } else {
                                    onUpdateReminderEnabled(true)
                                }
                            } else {
                                onUpdateReminderEnabled(false)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SuccessGreen)
                    )
                }
            }
        }

        item { SectionTitle("外观") }
        item { ThemeSelector(selectedTheme = uiState.theme, onThemeSelected = onUpdateTheme) }

        item { SectionTitle("数据管理") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { backupLauncher.launch("worklog_backup_${System.currentTimeMillis()}.json") }, modifier = Modifier.weight(1f)) { Text("备份") }
                        Button(onClick = { importLauncher.launch(arrayOf("application/json")) }, modifier = Modifier.weight(1f)) { Text("导入") }
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                SectionTitle("最近打卡记录")
                TextButton(onClick = onNavigateToAllSessions) { Text("更多") }
            }
        }
        items(uiState.recentSessions) { session ->
            SessionHistoryItem(session, onDeleteSession, onUpdateSession)
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// [新增] 权限引导弹窗
@Composable
fun PermissionGuideDialog(
    hasNotificationPerm: Boolean,
    isBatteryWhitelisted: Boolean,
    onDismiss: () -> Unit,
    onAllGranted: () -> Unit
) {
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("开启提醒功能", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "为了确保您在后台被清理后仍能收到打卡提醒，请授予以下权限：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(16.dp))

                PermissionItem(
                    title = "允许发送通知",
                    subtitle = "用于接收打卡提醒",
                    isGranted = hasNotificationPerm,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            PermissionUtils.openNotificationSettings(context)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                PermissionItem(
                    title = "允许应用自启动",
                    subtitle = "防止提醒被系统拦截",
                    isGranted = false,
                    showStatus = false,
                    onClick = { PermissionUtils.openAutoStartSettings(context) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                PermissionItem(
                    title = "电池优化白名单",
                    subtitle = "允许后台运行不被杀",
                    isGranted = isBatteryWhitelisted,
                    onClick = { PermissionUtils.requestIgnoreBatteryOptimizations(context) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                val allHardPermissionsGranted = hasNotificationPerm && isBatteryWhitelisted

                Button(
                    onClick = {
                        if (allHardPermissionsGranted) {
                            onAllGranted()
                        } else {
                            // 如果是用户点击了但系统权限还没刷新，或者不想开启，点击无效
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (allHardPermissionsGranted) SuccessGreen else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (allHardPermissionsGranted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(if (allHardPermissionsGranted) "已完成设置，开启提醒" else "请先开启上述权限")
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDismiss) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    showStatus: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }

        if (showStatus) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGranted) SuccessGreen else AlertRed,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// --- 以下是必须保留的辅助组件 ---

@Composable
fun WorkingDaysSelector(
    selectedDays: Set<DayOfWeek>,
    onSelectionChanged: (Set<DayOfWeek>) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val days = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        days.forEach { day ->
            val isSelected = day in selectedDays
            val label = day.getDisplayName(TextStyle.NARROW, Locale.CHINESE)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        val newSet = selectedDays.toMutableSet()
                        if (isSelected) newSet.remove(day) else newSet.add(day)
                        onSelectionChanged(newSet)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerItem(label: String, time: LocalTime, onTimeSelected: (LocalTime) -> Unit, modifier: Modifier = Modifier) {
    var showDialog by remember { mutableStateOf(false) }
    var showInputMode by remember { mutableStateOf(false) }

    if (showDialog) {
        val timePickerState = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = { TextButton(onClick = { onTimeSelected(LocalTime.of(timePickerState.hour, timePickerState.minute)); showDialog = false }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("取消") } },
            title = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("选择时间")
                    IconButton(onClick = { showInputMode = !showInputMode }) { Icon(if (showInputMode) Icons.Default.Schedule else Icons.Default.Keyboard, contentDescription = "Switch") }
                }
            },
            text = { if (showInputMode) TimeInput(state = timePickerState) else TimePicker(state = timePickerState) }
        )
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = time.format(DateTimeFormatter.ofPattern("HH:mm")), onValueChange = {}, label = { Text(label) },
            leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) }, readOnly = true,
            modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
        )
        Box(modifier = Modifier.matchParentSize().clickable { showDialog = true })
    }
}

@Composable
fun ThemeSelector(selectedTheme: Theme, onThemeSelected: (Theme) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Theme.entries.forEach { theme ->
            val isSelected = selectedTheme == theme
            Button(
                onClick = { onThemeSelected(theme) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = when(theme) { Theme.LIGHT -> Icons.Default.LightMode; Theme.DARK -> Icons.Default.DarkMode; Theme.SYSTEM -> Icons.Default.PhoneAndroid }, contentDescription = null)
                    Text(when(theme) { Theme.LIGHT -> "浅色"; Theme.DARK -> "深色"; Theme.SYSTEM -> "系统" })
                }
            }
        }
    }
}

@Composable
fun SessionHistoryItem(session: Session, onDelete: (Long) -> Unit, onUpdate: (Session) -> Unit) {
    var note by remember { mutableStateOf(TextFieldValue(session.note ?: "")) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(note.text) {
        if (note.text != (session.note ?: "")) {
            delay(800)
            onUpdate(session.copy(note = note.text))
        }
    }
    LaunchedEffect(session.note) {
        if (note.text != (session.note ?: "")) { note = TextFieldValue(session.note ?: "") }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条打卡记录吗？") },
            confirmButton = { TextButton(onClick = { onDelete(session.id); showDeleteDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("删除") } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("取消") } }
        )
    }

    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val startTime = session.startTime.atZone(ZoneId.systemDefault())
    val endTime = session.endTime?.atZone(ZoneId.systemDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(dateFormatter.format(startTime), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = session.isHoliday,
                        onCheckedChange = { onUpdate(session.copy(isHoliday = it)) },
                        modifier = Modifier.size(32.dp)
                    )
                    Text("今天是法定节假日", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (endTime != null) {
                    val duration = Duration.between(startTime, endTime)
                    val hours = duration.toHours()
                    val minutes = duration.toMinutes() % 60
                    Text(String.format("%d:%02d", hours, minutes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertLight) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = note, onValueChange = { note = it }, placeholder = { Text("添加备注...", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Transparent, focusedBorderColor = MaterialTheme.colorScheme.background)
            )
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp))
}