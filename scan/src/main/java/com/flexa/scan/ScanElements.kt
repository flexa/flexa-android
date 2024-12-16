package com.flexa.scan

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flexa.core.theme.FlexaTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class Holder(private val scope: CoroutineScope) {
    private val image = MutableSharedFlow<Bitmap>()
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
                    color = MaterialTheme.colorScheme.primary
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
    val previewMode = LocalInspectionMode.current
    var expanded by rememberSaveable { mutableStateOf(!previewMode) }
    val density = LocalDensity.current
    var size by remember { mutableStateOf(IntSize.Zero) }
    val height by animateFloatAsState(
        targetValue = if (expanded) density.run { size.height.toDp() }.value else 0f,
        animationSpec = tween(durationMillis = 500), label = ""
    )
    val width by animateFloatAsState(
        targetValue = if (expanded) density.run { size.width.toDp() }.value else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 200), label = ""
    )

    LaunchedEffect(Unit) {
        delay(300)
        expanded = true
    }

    Box(
        modifier = modifier
            .onGloballyPositioned {
                size = it.size
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
            val infiniteTransition = rememberInfiniteTransition(label = "")
            val padding by remember { mutableStateOf(32.dp) }
            val animatedOffset by infiniteTransition.animateFloat(
                initialValue = with(density) { (padding / 2.5f).toPx() },
                targetValue = width - with(density) { (padding / 2.5f).toPx() },
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 2000,
                        easing = EaseInOut,
                        delayMillis = 0
                    ),
                    repeatMode = RepeatMode.Reverse
                ), label = ""
            )
            val animatedColor by infiniteTransition.animateColor(
                initialValue = Color.White,
                targetValue = Color.White.copy(alpha = .3f),
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 1000,
                        easing = EaseInOut,
                        delayMillis = 0
                    ),
                    repeatMode = RepeatMode.Reverse
                ), label = ""
            )
            val glowHeight by remember { mutableStateOf(1.dp) }
            Canvas(
                modifier = Modifier
                    .padding(horizontal = padding)
                    .fillMaxWidth()
                    .height(glowHeight)
                    .offset(y = animatedOffset.dp)
            ) {
                val canvasWidth = this.size.width
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
                    strokeWidth = with(density) { glowHeight.toPx() },
                    cap = StrokeCap.Round
                )
            }
        }
    }
}


@Composable
internal fun FlashButton(
    modifier: Modifier = Modifier,
    enabled: Boolean,
) {
    val palette = MaterialTheme.colorScheme
    val height = remember { mutableFloatStateOf(0f) }
    Box(modifier = modifier.onGloballyPositioned {
        height.floatValue = it.size.height.toFloat()
    }) {
        Icon(
            modifier = Modifier.fillMaxSize(),
            imageVector = Icons.Rounded.FlashlightOn,
            contentDescription = null,
            tint = palette.onSurface
        )
        Switch(
            modifier = Modifier
                .align(Alignment.Center)
                .scale(height.floatValue * .002F)
                .rotate(270F)
                .offset(x = (-20).dp),
            colors = SwitchDefaults.colors(
                checkedTrackColor = palette.surface,
                checkedBorderColor = palette.surface,
                checkedThumbColor = palette.onSurface,
                uncheckedTrackColor = palette.surface,
                uncheckedBorderColor = palette.surface,
                uncheckedThumbColor = palette.onSurface
            ),
            thumbContent = {
                Spacer(modifier = Modifier.size(1.dp))
            },
            checked = enabled,
            onCheckedChange = {}
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun FlashButtonPreview() {
    FlexaTheme {
        Surface {
            Column {
                FlashButton(
                    modifier = Modifier.size(60.dp),
                    enabled = false
                )
            }
        }
    }
}