package com.example.worklog.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.worklog.ui.theme.BackgroundGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllSessionsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val allSessions by viewModel.allSessions.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("所有打卡记录", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .background(BackgroundGray)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(allSessions, key = { it.id }) { session ->
                // [修复] SessionHistoryItem 参数更新
                SessionHistoryItem(
                    session = session,
                    onDelete = { viewModel.deleteSession(it) },
                    onUpdate = { updatedSession -> viewModel.updateSession(updatedSession) }
                )
            }
        }
    }
}