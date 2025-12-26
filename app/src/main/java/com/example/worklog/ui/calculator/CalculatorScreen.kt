package com.example.worklog.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worklog.ui.theme.*

@Composable
fun CalculatorScreen() {
    var monthlySalary by remember { mutableStateOf("20000") }
    var months by remember { mutableStateOf("12") }
    var bonus by remember { mutableStateOf("0") }
    var stockQty by remember { mutableStateOf("0") }
    var stockPrice by remember { mutableStateOf("0") }
    var vestingYears by remember { mutableStateOf("1") }
    var otherCash by remember { mutableStateOf("0") }

    val stockValue = (stockQty.toIntOrNull() ?: 0) * (stockPrice.toDoubleOrNull() ?: 0.0)
    val annualStockValue = if ((vestingYears.toIntOrNull() ?: 1) > 0) stockValue / (vestingYears.toIntOrNull() ?: 1) else 0.0

    val totalPackage = (monthlySalary.toDoubleOrNull() ?: 0.0) *
            (months.toIntOrNull() ?: 12) +
            (bonus.toDoubleOrNull() ?: 0.0) +
            annualStockValue +
            (otherCash.toDoubleOrNull() ?: 0.0)

    val formula = "年薪 = 月薪 × 月数 + 年终奖金 + (股票数量 × 单股价格 / 归属年限) + 其他现金"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "年薪计算器",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 24.dp)
        )

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
                    text = "预估年总包",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
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
                    InputItem(
                        value = months,
                        onValueChange = { months = it },
                        label = "月数",
                        modifier = Modifier.weight(0.4f)
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
                InputItem(value = vestingYears, onValueChange = { vestingYears = it }, label = "股票归属年限")
                Spacer(modifier = Modifier.height(12.dp))
                InputItem(value = otherCash, onValueChange = { otherCash = it }, label = "其他现金", prefix = "¥")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = formula,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun InputItem(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    prefix: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        leadingIcon = prefix?.let { { Text(it, color = MaterialTheme.colorScheme.onSurface) } },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}