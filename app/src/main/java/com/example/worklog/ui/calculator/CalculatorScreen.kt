package com.example.worklog.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worklog.ui.theme.*

@Composable
fun CalculatorScreen() {
    // 1. [优化] 初始值改为空字符串 ""
    // 使用 remember 确保数据仅在 App 存活期间保留，杀后台重启后会自动重置
    var monthlySalary by remember { mutableStateOf("") }
    var months by remember { mutableStateOf("") }
    var bonus by remember { mutableStateOf("") }
    var stockQty by remember { mutableStateOf("") }
    var stockPrice by remember { mutableStateOf("") }
    var vestingYears by remember { mutableStateOf("") }
    var otherCash by remember { mutableStateOf("") }

    // 实时计算逻辑 (处理空字符串转数字的安全问题)
    val stockValue = (stockQty.toIntOrNull() ?: 0) * (stockPrice.toDoubleOrNull() ?: 0.0)
    val vesting = vestingYears.toIntOrNull() ?: 1
    // 避免除以0
    val safeVestingYears = if (vesting > 0) vesting else 1
    val annualStockValue = stockValue / safeVestingYears

    val totalPackage = (monthlySalary.toDoubleOrNull() ?: 0.0) * (months.toDoubleOrNull() ?: 12.0) +
            (bonus.toDoubleOrNull() ?: 0.0) +
            annualStockValue +
            (otherCash.toDoubleOrNull() ?: 0.0)

    val formula = "算法：月薪×发薪月数 + 年终奖 + (股数×股价/归属年) + 现金"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 标题栏 + 重置按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "年薪计算器",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            // [优化] 添加重置按钮
            Button(
                onClick = {
                    monthlySalary = ""
                    months = ""
                    bonus = ""
                    stockQty = ""
                    stockPrice = ""
                    vestingYears = ""
                    otherCash = ""
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("重置", fontSize = 12.sp)
            }
        }

        // 结果卡片
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "预估年总包 (Total Package)",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 如果没有输入任何数据，显示 0
                Text(
                    text = "¥ ${String.format("%.0f", totalPackage)}",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "*税前估算, 仅供参考",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 输入区域
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InputItem(
                        value = monthlySalary,
                        onValueChange = { monthlySalary = it },
                        label = "月薪 (BASE)",
                        modifier = Modifier.weight(1f),
                        prefix = "¥"
                    )
                    // [修改] 文案改为“发薪月数”
                    InputItem(
                        value = months,
                        onValueChange = { months = it },
                        label = "发薪月数",
                        modifier = Modifier.weight(0.4f),
                        placeholder = "12"
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                InputItem(value = bonus, onValueChange = { bonus = it }, label = "年终奖金", prefix = "¥")
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InputItem(
                        value = stockQty,
                        onValueChange = { stockQty = it },
                        label = "股票数量",
                        modifier = Modifier.weight(1f)
                    )
                    InputItem(
                        value = stockPrice,
                        onValueChange = { stockPrice = it },
                        label = "单股价格",
                        modifier = Modifier.weight(1f),
                        prefix = "¥"
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                InputItem(
                    value = vestingYears,
                    onValueChange = { vestingYears = it },
                    label = "股票归属年限",
                    placeholder = "1"
                )
                Spacer(modifier = Modifier.height(12.dp))
                InputItem(value = otherCash, onValueChange = { otherCash = it }, label = "其他现金 (签字费/房补等)", prefix = "¥")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = formula,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun InputItem(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    prefix: String? = null,
    placeholder: String = ""
) {
    val numValue = value.toDoubleOrNull() ?: 0.0
    val suffixText = if (numValue >= 10000) {
        val isExact = numValue % 1000 == 0.0
        val prefixText = if (isExact) "=" else "≈"
        "$prefixText${String.format("%.1f", numValue / 10000)}万"
    } else null

    OutlinedTextField(
        value = value,
        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) onValueChange(it) },
        label = { Text(label, fontSize = 12.sp) },
        placeholder = { if(placeholder.isNotEmpty()) Text(placeholder, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        leadingIcon = prefix?.let { { Text(it, color = MaterialTheme.colorScheme.onSurface) } },
        suffix = suffixText?.let { { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary) } },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}