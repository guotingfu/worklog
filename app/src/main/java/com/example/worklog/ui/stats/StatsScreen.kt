package com.example.worklog.ui.stats

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.worklog.ui.theme.*
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.compose.component.textComponent
import com.patrykandpatrick.vico.compose.dimensions.dimensionsOf
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.dimensions.HorizontalDimensions
import com.patrykandpatrick.vico.core.chart.insets.Insets
import com.patrykandpatrick.vico.core.chart.layout.HorizontalLayout
import com.patrykandpatrick.vico.core.component.marker.MarkerComponent
import com.patrykandpatrick.vico.core.component.shape.DashedShape
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.context.MeasureContext
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.marker.Marker

@Composable
fun StatsScreen(viewModel: StatsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val wageFormat = "%.2f"

    if (uiState.errorMessage != null) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Error, contentDescription = null, tint = AlertRed, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("统计页面发生错误", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.errorMessage ?: "未知错误",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        StatsHeader(uiState.selectedUnit, onUnitChange = { viewModel.setUnit(it) })

        if (uiState.standardWorkHours < 8.0) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = AlertRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "当前设置标准工作时间为 ${String.format("%.1f", uiState.standardWorkHours)} 小时，请确认是否设置有误",
                        color = AlertRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.weight(1f, fill = false)) {
            ChartOverviewCard(uiState, onPeriodChange = { viewModel.setPeriod(it) })
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(IntrinsicSize.Min)
        ) {
            DataCard("总工时", String.format("%.1f", uiState.totalWorkDuration), uiState.selectedUnit.name.lowercase(), Modifier.weight(1f).fillMaxHeight())
            DataCard(
                title = if(uiState.selectedUnit == TimeDisplayUnit.HOUR) "实际时薪" else "实际分薪",
                value = "¥${String.format(wageFormat, uiState.actualWage)}",
                unit = "",
                modifier = Modifier.weight(1f).fillMaxHeight(),
                isStandard = uiState.isWageStandard
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        OvertimeCard(uiState.overtimeDuration, uiState.selectedUnit)
        Spacer(modifier = Modifier.height(12.dp))
        InfoFooter()
    }
}

@Composable
fun StatsHeader(selectedUnit: TimeDisplayUnit, onUnitChange: (TimeDisplayUnit) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("工时统计", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
        Row {
            UnitSelectorButton("时", selectedUnit == TimeDisplayUnit.HOUR) { onUnitChange(TimeDisplayUnit.HOUR) }
            Spacer(modifier = Modifier.width(8.dp))
            UnitSelectorButton("分", selectedUnit == TimeDisplayUnit.MINUTE) { onUnitChange(TimeDisplayUnit.MINUTE) }
        }
    }
}

@Composable
fun UnitSelectorButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        modifier = Modifier.height(32.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
    }
}

@Composable
fun ChartOverviewCard(uiState: StatsUiState, onPeriodChange: (StatsPeriod) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("概览", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PeriodSelectorButton("本周", uiState.selectedPeriod == StatsPeriod.WEEK) { onPeriodChange(StatsPeriod.WEEK) }
                    PeriodSelectorButton("本月", uiState.selectedPeriod == StatsPeriod.MONTH) { onPeriodChange(StatsPeriod.MONTH) }
                    PeriodSelectorButton("全年", uiState.selectedPeriod == StatsPeriod.YEAR) { onPeriodChange(StatsPeriod.YEAR) }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.height(160.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.chartData.entries.isEmpty() || uiState.chartData.entries.all { it.isEmpty() }) {
                Box(modifier = Modifier.height(160.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("暂无数据", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                }
            } else {
                // [修复] 使用 remember 创建单例 producer，并用 LaunchedEffect 更新数据
                // 这样避免了 Recomposition 时频繁销毁重建导致图表无法绘制的问题
                val producer = remember { ChartEntryModelProducer() }
                LaunchedEffect(uiState.chartData.entries) {
                    producer.setEntries(uiState.chartData.entries)
                }

                val (barThickness, barSpacing, labelStrategy) = when (uiState.selectedPeriod) {
                    StatsPeriod.WEEK -> Triple(20.dp, 12.dp, LabelStrategy.SHOW_ALL)
                    StatsPeriod.MONTH -> Triple(6.dp, 2.dp, LabelStrategy.SPARSE)
                    StatsPeriod.YEAR -> Triple(14.dp, 6.dp, LabelStrategy.SHOW_ALL)
                }

                val marker = rememberMarker()

                Chart(
                    chart = columnChart(
                        columns = listOf(
                            LineComponent(
                                color = AlertRed.toArgb(),
                                thicknessDp = barThickness.value,
                                shape = Shapes.pillShape
                            ),
                            LineComponent(
                                color = SuccessGreen.toArgb(),
                                thicknessDp = barThickness.value,
                                shape = Shapes.pillShape
                            ),
                        ),
                        spacing = barSpacing,
                        mergeMode = com.patrykandpatrick.vico.core.chart.column.ColumnChart.MergeMode.Stack
                    ),
                    chartModelProducer = producer,
                    modifier = Modifier.height(160.dp),
                    horizontalLayout = HorizontalLayout.FullWidth(),
                    marker = marker,
                    startAxis = rememberStartAxis(
                        label = textComponent(color = Color.Transparent),
                        axis = null,
                        guideline = null
                    ),
                    bottomAxis = rememberBottomAxis(
                        label = textComponent(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), textSize = 10.sp),
                        axis = LineComponent(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f).toArgb()),
                        valueFormatter = AxisValueFormatter { value, _ ->
                            val index = value.toInt()
                            val label = uiState.chartData.labels.getOrElse(index) { "" }
                            when (labelStrategy) {
                                LabelStrategy.SHOW_ALL -> label
                                LabelStrategy.SPARSE -> {
                                    if (index == 0 || (index + 1) % 5 == 0) label else ""
                                }
                            }
                        }
                    ),
                )
            }
        }
    }
}

enum class LabelStrategy { SHOW_ALL, SPARSE }

@Composable
fun PeriodSelectorButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp)),
        colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
    ) {
        Text(text, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
    }
}

@Composable
fun DataCard(title: String, value: String, unit: String, modifier: Modifier = Modifier, isStandard: Boolean? = null) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Text(unit, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 2.dp))
            }
            isStandard?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (it) Icons.Default.CheckCircle else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = if (it) SuccessGreen else AlertRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (it) "标准" else "下降", color = if (it) SuccessGreen else AlertRed, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun OvertimeCard(overtime: Double, unit: TimeDisplayUnit) {
    Card(colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.1f)), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("加班统计", color = AlertRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(String.format("本周期额外工作了 %.1f ${unit.name.lowercase()}", overtime), color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
        }
    }
}

@Composable
fun InfoFooter() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "算法说明",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "1. 年工作日 = (每周工作天数 × 52周) - 11天法定节假日。\n" +
                        "2. 标准时薪 = 年薪 ÷ (年工作日 × 标准日工时)。\n" +
                        "3. 实际时薪 = 周期应发薪资 ÷ 有效总工时。\n" +
                        "   • 周期薪资 = 年薪 × (周期理论工作日 ÷ 年工作日)\n" +
                        "   • 有效工时 = Max(实际总工时, 标准总工时)\n" +
                        "4. 本软件不鼓励摸鱼和早退，早退按标准工时计算，避免时薪虚高；加班时间计入实际工时计算，体现薪资稀释。",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
internal fun rememberMarker(): Marker {
    val labelBackgroundColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val label = textComponent(
        color = onSurfaceColor,
        textSize = 12.sp,
        typeface = Typeface.MONOSPACE,
        padding = dimensionsOf(8.dp, 4.dp),
        background = shapeComponent(shape = Shapes.pillShape, color = labelBackgroundColor)
    )

    val guideline = LineComponent(
        color = onSurfaceColor.copy(alpha = 0.4f).toArgb(),
        thicknessDp = 1f,
        shape = DashedShape(
            shape = Shapes.pillShape,
            dashLengthDp = 4f,
            gapLengthDp = 2f
        )
    )

    return remember(label, guideline) {
        object : MarkerComponent(label, guideline, null) {
            override fun getInsets(context: MeasureContext, outInsets: Insets, horizontalDimensions: HorizontalDimensions) {
                with(context) {
                    outInsets.top = label.getHeight(context)
                }
            }
        }
    }
}

private val Float?.orZero: Float
    get() = this ?: 0f