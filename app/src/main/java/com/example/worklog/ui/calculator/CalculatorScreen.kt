package com.example.worklog.ui.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.worklog.ui.theme.WorkLogTheme

@Composable
fun CalculatorScreen() {
    var monthlySalary by remember { mutableStateOf("") }
    var months by remember { mutableStateOf("12") }
    var bonus by remember { mutableStateOf("") }
    var stockQty by remember { mutableStateOf("") }
    var stockPrice by remember { mutableStateOf("") }
    var other by remember { mutableStateOf("") }

    val totalPackage = (monthlySalary.toDoubleOrNull() ?: 0.0) *
        (months.toIntOrNull() ?: 12) +
        (bonus.toDoubleOrNull() ?: 0.0) +
        ((stockQty.toIntOrNull() ?: 0) * (stockPrice.toDoubleOrNull() ?: 0.0)) +
        (other.toDoubleOrNull() ?: 0.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = monthlySalary,
            onValueChange = { monthlySalary = it },
            label = { Text("月薪 (Base)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = months,
            onValueChange = { months = it },
            label = { Text("月数 (Months)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = bonus,
            onValueChange = { bonus = it },
            label = { Text("年终奖金 (Bonus)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = stockQty,
            onValueChange = { stockQty = it },
            label = { Text("股票数量 (Stock Qty)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = stockPrice,
            onValueChange = { stockPrice = it },
            label = { Text("单股价格 (Stock Price)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = other,
            onValueChange = { other = it },
            label = { Text("签字费/其他 (Other)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "预估年总包 (Total Package)")
                Text(text = "¥${String.format("%.2f", totalPackage)}")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CalculatorScreenPreview() {
    WorkLogTheme {
        CalculatorScreen()
    }
}
