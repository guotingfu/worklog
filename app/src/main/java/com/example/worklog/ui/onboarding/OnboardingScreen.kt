package com.example.worklog.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(onCompleted: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("欢迎使用 WorkLog", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))
        FeatureItem(
            icon = Icons.Default.CloudOff,
            title = "完全离线",
            description = "所有数据都安全地存储在您的设备上，无需网络连接。"
        )
        FeatureItem(
            icon = Icons.Default.Security,
            title = "隐私安全",
            description = "我们绝不收集或分享您的任何个人数据，您的薪资信息只有您自己知道。"
        )
        FeatureItem(
            icon = Icons.Default.AttachMoney,
            title = "反向时薪",
            description = "通过量化您的真实时薪，帮助您重新审视工作与生活的平衡。"
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onCompleted,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("开始使用")
        }
    }
}

@Composable
fun FeatureItem(icon: ImageVector, title: String, description: String) {
    Row(modifier = Modifier.padding(vertical = 16.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}