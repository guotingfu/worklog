package com.example.worklog.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LongPressButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    color: Color
) {
    var isPressed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var progress by remember { mutableStateOf(0f) }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = if (progress > 0) 1500 else 0),
        label = "progress"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(200.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        coroutineScope.launch {
                            progress = 1f
                            delay(1500)
                            if (isPressed) {
                                onClick()
                            }
                        }
                        tryAwaitRelease()
                        isPressed = false
                        progress = 0f
                    }
                )
            }
    ) {
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.size(200.dp),
            color = color,
            strokeWidth = 8.dp
        )
        Text(
            text = text,
            color = color,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
    }
}