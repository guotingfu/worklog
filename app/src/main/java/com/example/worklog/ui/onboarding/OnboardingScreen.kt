package com.example.worklog.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource // [新增引用]
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.worklog.R // 确保引用了R文件

@Composable
fun OnboardingScreen(onDismiss: (Boolean) -> Unit) {
    var doNotShowAgain by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 使用 stringResource
                Text(
                    text = stringResource(R.string.onboarding_welcome_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    FeatureItem(
                        icon = Icons.Default.CloudOff,
                        title = stringResource(R.string.feature_offline_title),
                        description = stringResource(R.string.feature_offline_desc)
                    )
                    FeatureItem(
                        icon = Icons.Default.Security,
                        title = stringResource(R.string.feature_privacy_title),
                        description = stringResource(R.string.feature_privacy_desc)
                    )
                    FeatureItem(
                        icon = Icons.Default.AttachMoney,
                        title = stringResource(R.string.feature_hourly_rate_title),
                        description = stringResource(R.string.feature_hourly_rate_desc)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Checkbox(
                        checked = doNotShowAgain,
                        onCheckedChange = { doNotShowAgain = it }
                    )
                    Text(
                        text = stringResource(R.string.onboarding_dont_show_again),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }

                Button(
                    onClick = { onDismiss(doNotShowAgain) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.onboarding_start_button), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FeatureItem(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}