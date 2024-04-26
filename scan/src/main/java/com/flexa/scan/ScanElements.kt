package com.flexa.scan

import android.graphics.Bitmap
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class Holder(val scope: CoroutineScope) {
    val image = MutableSharedFlow<Bitmap>()
    val codes = MutableSharedFlow<List<String>>()
    fun setImage(img: Bitmap) {
        scope.launch { image.emit(img) }
    }

    fun setCodes(codes: List<String>) {
        scope.launch { this@Holder.codes.emit(codes) }
    }
}

@Composable
fun KeepScreenOn() {
    val currentView = LocalView.current
    DisposableEffect(Unit) {
        currentView.keepScreenOn = true
        onDispose {
            currentView.keepScreenOn = false
        }
    }
}

@Composable
fun TopContent(
    modifier: Modifier
) {
    Box(
        modifier = modifier
    ) {
        Column(
            Modifier
                .width(200.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(id = R.string.scan_any_code),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(id = R.string.send_pay_connect),
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun Codes(
    modifier: Modifier = Modifier,
    holder: Holder = Holder(CoroutineScope(Dispatchers.Main))
) {
    val codes = holder.codes.collectAsStateWithLifecycle(emptyList())

    LazyColumn(modifier = modifier) {
        codes.value.forEachIndexed { _, s ->
            item {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(Color.Red)) {
                            append("> ")
                        }
                        withStyle(style = SpanStyle(Color.White)) {
                            append(s)
                        }
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun ScannerBox(
    modifier: Modifier = Modifier,
    onGloballyPositioned: (LayoutCoordinates) -> Unit
) {
    var expanded by rememberSaveable {
        mutableStateOf(false)
    }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var size by remember { mutableStateOf(IntSize.Zero) }
    val height by animateFloatAsState(
        targetValue = if (expanded) {
            density.run { size.height.toDp() }.value
        } else 0f,
        animationSpec = tween(
            durationMillis = 500,
        )
    )
    val width by animateFloatAsState(
        targetValue = if (expanded)
            density.run { size.width.toDp() }.value else 0f,
        animationSpec = tween(
            durationMillis = 500,
            delayMillis = 200
        )
    )
    val infiniteTransition = rememberInfiniteTransition()
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = width - (40.dp.value * 2),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                easing = EaseInOut,
                delayMillis = 0
            ),
            repeatMode = RepeatMode.Reverse
        )
    )
    val animatedColor by infiniteTransition.animateColor(
        initialValue = Color.White,
        targetValue = Color.White.copy(alpha = .5f),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = EaseInOut,
                delayMillis = 0
            ),
            repeatMode = RepeatMode.Reverse
        )

    )

    Box(
        modifier = modifier
            .onGloballyPositioned {
                size = it.size
                scope.launch(Dispatchers.Main) {
                    delay(1000)
                    expanded = true
                }
                onGloballyPositioned.invoke(it)
            }
    ) {
        Box(
            Modifier
                .align(Alignment.Center)
                .height(height.dp)
                .width(width.dp)
                .border(
                    BorderStroke(4.dp, Color.White),
                    RoundedCornerShape(36.dp)
                )
        ) {
            Canvas(
                modifier = Modifier
                    .padding(40.dp)
                    .fillMaxWidth()
                    .offset(y = animatedOffset.dp)
            ) {
                val canvasWidth = this.size.width
                val canvasHeight = this.size.height
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            animatedColor,
                            Color.Transparent
                        )
                    ),
                    start = Offset(x = 0f, y = 0f),
                    end = Offset(x = canvasWidth, y = 0f),
                    strokeWidth = 1.dp.value
                )
            }
        }
    }
}
