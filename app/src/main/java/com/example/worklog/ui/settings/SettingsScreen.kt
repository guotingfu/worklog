package com.example.worklog.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.worklog.data.local.db.Session
import com.example.worklog.data.local.datastore.Theme
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onUpdateAnnualSalary: (String) -> Unit,
    onUpdateTheme: (Theme) -> Unit,
    onUpdateStartTime: (String) -> Unit,
    onUpdateEndTime: (String) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onUpdateSessionNote: (Session, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text("工作配置", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            OutlinedTextField(
                value = uiState.annualSalary,
                onValueChange = onUpdateAnnualSalary,
                label = { Text("年薪") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.startTime,
                    onValueChange = onUpdateStartTime,
                    label = { Text("标准上班时间") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = uiState.endTime,
                    onValueChange = onUpdateEndTime,
                    label = { Text("标准下班时间") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text("外观", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            ThemeSelector(selectedTheme = uiState.theme, onThemeSelected = onUpdateTheme)
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text("数据管理", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        }

        items(uiState.recentSessions) { session ->
            SessionItem(session, onDeleteSession, onUpdateSessionNote)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelector(selectedTheme: Theme, onThemeSelected: (Theme) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedTheme.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("主题") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Theme.entries.forEach { theme ->
                DropdownMenuItem(
                    text = { Text(theme.name) },
                    onClick = {
                        onThemeSelected(theme)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SessionItem(session: Session, onDelete: (Long) -> Unit, onUpdateNote: (Session, String) -> Unit) {
    var note by remember { mutableStateOf(TextFieldValue(session.note ?: "")) }
    val formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "上班: ${session.startTime.let { formatter.format(it.atZone(java.time.ZoneId.systemDefault())) }}")
                    Text(text = "下班: ${session.endTime?.let { formatter.format(it.atZone(java.time.ZoneId.systemDefault())) } ?: "-"}")
                }
                IconButton(onClick = { onDelete(session.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Session")
                }
            }
            OutlinedTextField(
                value = note,
                onValueChange = { 
                    note = it
                    onUpdateNote(session, it.text)
                 },
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
