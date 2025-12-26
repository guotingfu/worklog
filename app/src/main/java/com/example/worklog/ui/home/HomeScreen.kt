package com.example.worklog.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worklog.ui.theme.*
import kotlinx.coroutines.delay
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TopHeader(isWorking = isWorking)

            TimerRing(isWorking = isWorking, timerText = timer)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LongPressActionButton(
                    isWorking = isWorking,
                    onLongPressComplete = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isWorking) {
                            showConfetti = true
                        }
                        onToggleWork()
                    }
                )
                Spacer(modifier = Modifier.height(40.dp))
                SalaryInfoCard(
                    salary = annualSalary,
                    isVisible = isSalaryVisible,
                    onToggleVisibility = onToggleSalaryVisibility
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showConfetti) {
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = remember { listOf(party) }
            )
            LaunchedEffect(showConfetti) {
                if (showConfetti) {
                    delay(4000)
                    showConfetti = false
                }
            }
        }
    }
}

@Composable
fun TopHeader(isWorking: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (isWorking) StatusGreen else StatusGray,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isWorking) "工作中" else "未打卡",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isWorking) "保持专注" else "休息时间",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
    }
}


@Composable
fun TimerRing(isWorking: Boolean, timerText: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = timerText,
            fontSize = 56.sp, 
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isWorking) "本次工作时长" else "准备开始",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun SalaryInfoCard(salary: String, isVisible: Boolean, onToggleVisibility: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "年薪", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isVisible) "¥ $salary" else "¥ ******",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(
                onClick = onToggleVisibility,
                modifier = Modifier.background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Toggle Visibility",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun LongPressActionButton(
    isWorking: Boolean,
    onLongPressComplete: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = if (isPressed) {
            tween(durationMillis = 1500, easing = LinearEasing)
        } else {
            tween(durationMillis = 200)
        },
        label = "Progress"
    )

    LaunchedEffect(animatedProgress) {
        if (animatedProgress >= 1f && isPressed) {
            onLongPressComplete()
            isPressed = false
            progress = 0f
        }
    }

    val buttonBrush = if (isWorking) {
        Brush.horizontalGradient(listOf(AlertRed, AlertLight))
    } else {
        Brush.horizontalGradient(listOf(PrimaryBlue, PrimaryLight))
    }

    val buttonSize = 160.dp
    val innerButtonSize = 140.dp
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(buttonSize)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (isPressed || animatedProgress > 0) {
                drawCircle(
                    color = onBackgroundColor.copy(alpha = 0.1f),
                    style = Stroke(width = 10.dp.toPx())
                )
                drawArc(
                    color = if(isWorking) AlertRed else PrimaryBlue,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(innerButtonSize)
                .shadow(12.dp, CircleShape, spotColor = if (isWorking) AlertRed else PrimaryBlue)
                .clip(CircleShape)
                .background(buttonBrush)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isPressed = true
                            progress = 1f
                            tryAwaitRelease()
                            isPressed = false
                            progress = 0f
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isWorking) "下班" else "上班",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isWorking) "HOLD TO STOP" else "HOLD TO START",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
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
