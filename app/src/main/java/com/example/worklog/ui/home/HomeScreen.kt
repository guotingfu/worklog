package com.example.worklog.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worklog.ui.theme.Indigo500
import com.example.worklog.ui.theme.Red500
import com.example.worklog.ui.theme.WorkLogTheme
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@Composable
fun HomeScreen(
    isWorking: Boolean,
    timer: String,
    annualSalary: String,
    isSalaryVisible: Boolean,
    onToggleWork: () -> Unit,
    onToggleSalaryVisibility: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var showConfetti by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年M月d日")),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isWorking) {
                        PulseAnimation()
                    }
                    Text(
                        text = if (isWorking) "工作中" else "未打卡",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = timer,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            val buttonColor by animateColorAsState(if (isWorking) Red500 else Indigo500, label = "button color")

            LongPressButton(
                text = if (isWorking) "下班" else "上班",
                color = buttonColor,
                onClick = {
                    // Trigger confetti only when stopping work
                    if (isWorking) {
                        showConfetti = true
                    }
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleWork()
                }
            )

            SalaryCard(salary = annualSalary, isVisible = isSalaryVisible, onToggleVisibility = onToggleSalaryVisibility)
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showConfetti) {
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = remember { listOf(party) },
                updateListener = object : KonfettiView.UpdateListener {
                    override fun onUpdate(view: KonfettiView, activeParticles: Int) {
                        if (activeParticles == 0) showConfetti = false
                    }
                }
            )
        }
    }
}

@Composable
fun SalaryCard(salary: String, isVisible: Boolean, onToggleVisibility: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "年薪", style = MaterialTheme.typography.bodyLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isVisible) "¥ $salary" else "¥ ****",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp)
                )
                IconButton(onClick = onToggleVisibility, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Salary Visibility"
                    )
                }
            }
        }
    }
}

@Composable
fun PulseAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse scale"
    )
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .size(10.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Red500)
    )
}

private val party = Party(
    speed = 0f,
    maxSpeed = 30f,
    damping = 0.9f,
    spread = 360,
    colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def).map { it.toInt() },
    emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
    position = Position.Relative(0.5, 0.3)
)

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    WorkLogTheme {
        HomeScreen(
            isWorking = true,
            timer = "01:23:45",
            annualSalary = "300,000.00",
            isSalaryVisible = true,
            onToggleWork = {},
            onToggleSalaryVisibility = {}
        )
    }
}
