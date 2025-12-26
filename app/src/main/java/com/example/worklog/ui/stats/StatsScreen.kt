package com.example.worklog.ui.stats

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worklog.ui.theme.Green500
import com.example.worklog.ui.theme.Indigo500
import com.example.worklog.ui.theme.Red500
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.component.shape.LineComponent
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.column.ColumnChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.patrykandpatrick.vico.core.entry.plus

@Composable
fun StatsScreen(
    uiState: StatsUiState,
    onFilterSelected: (Filter) -> Unit,
    onTimeUnitSelected: (TimeUnit) -> Unit
) {
    val chartModelProducer = remember { ChartEntryModelProducer() }
    chartModelProducer.setEntries(uiState.chartData.values.map { entryOf(it.regular.toHours().toFloat(), it.overtime.toHours().toFloat()) })
    val bottomAxisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, chartValues ->
        (chartValues.chartEntryModel.entries.first().getOrNull(value.toInt()) as? com.patrykandpatrick.vico.core.entry.FloatEntry)?.let {
            uiState.chartData.keys.toList()[value.toInt()]
        } ?: ""
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        FilterChips(selectedFilter = uiState.filter, onFilterSelected = onFilterSelected)
        Spacer(modifier = Modifier.height(16.dp))
        Chart(chart = columnChart(
            columns = listOf(
                LineComponent(color = Indigo500.hashCode(), thickness = 8.dp),
                LineComponent(color = Red500.hashCode(), thickness = 8.dp)
            ),
            stacking = ColumnChart.Stacking.Stacked
        ), chartModelProducer = chartModelProducer,
            startAxis = rememberStartAxis(),
            bottomAxis = rememberBottomAxis(valueFormatter = bottomAxisValueFormatter)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            InfoCard("总工时", "${uiState.totalWorkHours.total.toHours()}h")
            RealHourlyWageCard(uiState.realHourlyWage, uiState.wageDiffPercentage)
        }
        if (uiState.overtimeSummary.isNotEmpty()) {
            Text(uiState.overtimeSummary, modifier = Modifier.padding(top = 16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChips(selectedFilter: Filter, onFilterSelected: (Filter) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Filter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.name) }
            )
        }
    }
}

@Composable
fun InfoCard(title: String, value: String) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RealHourlyWageCard(realHourlyWage: Double, wageDiffPercentage: Double) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("实际时薪", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "¥${String.format("%.2f", realHourlyWage)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                val (icon, color) = if (wageDiffPercentage >= 0) {
                    Pair(Icons.Default.ArrowUpward, Green500)
                } else {
                    Pair(Icons.Default.ArrowDownward, Red500)
                }
                Icon(icon, contentDescription = null, tint = color)
                Text("${String.format("%.2f", wageDiffPercentage)}%", color = color)
            }
        }
    }
}
