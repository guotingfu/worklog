package com.example.worklog.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worklog.data.local.db.Session
import com.example.worklog.data.local.datastore.Theme
import com.example.worklog.ui.theme.*
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onUpdateAnnualSalary: (String) -> Unit,
    onUpdateTheme: (Theme) -> Unit,
    onUpdateStartTime: (LocalTime) -> Unit,
    onUpdateEndTime: (LocalTime) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onUpdateSessionNote: (Session, String) -> Unit,
    onBackupData: (Uri) -> Unit,
    onImportData: (Uri) -> Unit,
    onCleanInvalidData: () -> Unit,
    onNavigateToAllSessions: () -> Unit
) {
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri -> if (uri != null) onBackupData(uri) }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> if (uri != null) onImportData(uri) }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SectionTitle("工作配置") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = uiState.annualSalary,
                        onValueChange = onUpdateAnnualSalary,
                        label = { Text("年薪 (元)") },
                        leadingIcon = { Text("¥") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimePicker(label = "上班时间", time = uiState.startTime, onTimeSelected = onUpdateStartTime, modifier = Modifier.weight(1f))
                        TimePicker(label = "下班时间", time = uiState.endTime, onTimeSelected = onUpdateEndTime, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item { SectionTitle("外观") }
        item {
            ThemeSelector(selectedTheme = uiState.theme, onThemeSelected = onUpdateTheme)
        }

        item { SectionTitle("数据管理") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { backupLauncher.launch("worklog_backup_${System.currentTimeMillis()}.json") },
                            modifier = Modifier.weight(1f)
                        ) { Text("备份 (JSON)") }
                        Button(
                            onClick = { importLauncher.launch(arrayOf("application/json")) },
                            modifier = Modifier.weight(1f)
                        ) { Text("导入") }
                    }
                    Button(
                        onClick = onCleanInvalidData,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("清理异常数据") }
                }
            }
        }

        item {
             Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                SectionTitle("最近打卡记录")
                TextButton(onClick = onNavigateToAllSessions) {
                    Text("更多")
                }
            }
        }
        items(uiState.recentSessions) { session ->
            SessionHistoryItem(session, onDeleteSession, onUpdateSessionNote)
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
fun TimePicker(label: String, time: LocalTime, onTimeSelected: (LocalTime) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val timePickerDialog = remember { android.app.TimePickerDialog(
        context,
        { _, hour, minute -> onTimeSelected(LocalTime.of(hour, minute)) },
        time.hour,
        time.minute,
        true
    )}

    Box(modifier = modifier) {
        OutlinedTextField(
            value = time.format(DateTimeFormatter.ofPattern("HH:mm")),
            onValueChange = {}, 
            label = { Text(label) },
            leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { timePickerDialog.show() }
        )
    }
}

@Composable
fun ThemeSelector(selectedTheme: Theme, onThemeSelected: (Theme) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Theme.entries.forEach { theme ->
            val isSelected = selectedTheme == theme
            Button(
                onClick = { onThemeSelected(theme) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = when(theme) {
                        Theme.LIGHT -> Icons.Default.LightMode
                        Theme.DARK -> Icons.Default.DarkMode
                        Theme.SYSTEM -> Icons.Default.PhoneAndroid
                    }, contentDescription = null)
                    Text(when(theme) {
                        Theme.LIGHT -> "浅色"
                        Theme.DARK -> "深色"
                        Theme.SYSTEM -> "系统"
                    })
                }
            }
        }
    }
}

@Composable
fun SessionHistoryItem(session: Session, onDelete: (Long) -> Unit, onUpdateNote: (Session, String) -> Unit) {
    var note by remember { mutableStateOf(TextFieldValue(session.note ?: "")) }
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
                Text(
                    text = dateFormatter.format(startTime),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (endTime != null) {
                    val duration = Duration.between(startTime, endTime)
                    val hours = duration.toHours()
                    val minutes = duration.toMinutes() % 60
                    val seconds = duration.seconds % 60
                    Text(
                        text = String.format("%d:%02d:%02d", hours, minutes, seconds),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { onDelete(session.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertLight)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { 
                    note = it
                    onUpdateNote(session, it.text) 
                },
                placeholder = { Text("添加备注...", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.background
                )
            )
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    )
}
