package com.flexa.core.view

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FlexaProgress(
    modifier: Modifier = Modifier,
    roundedCornersSize: Dp = 0.dp,
    borderWidth: Dp = 2.dp,
    borderColor: Color = Color.White,
    colors: List<Color> = listOf(Color.White, Color.Magenta, Color.Blue)
) {
    val infiniteTransition = rememberInfiniteTransition()
    val animatedCorners by infiniteTransition.animateFloat(
        initialValue = with(LocalDensity.current) { roundedCornersSize.toPx() },
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 500,
                delayMillis = 0
            ),
            repeatMode = RepeatMode.Reverse
        )
    )
    val animatedAngle by infiniteTransition.animateFloat(
        initialValue = 180f,
        targetValue = 360f + 180f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = EaseInOutSine, delayMillis = 0),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .rotate(animatedAngle)
            .background(
                brush = Brush.sweepGradient(colors = colors),
                shape = RoundedCornerShape(animatedCorners)
            )
            .border(
                width = borderWidth,
                shape = RoundedCornerShape(animatedCorners),
                brush = if (borderWidth.value > 0)
                    Brush.linearGradient(colors = listOf(borderColor, borderColor))
                else Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent))
            )
    )
}

@Preview
@Composable
private fun FlexaProgressPreview() {
    FlexaProgress(
        modifier = Modifier
            .size(72.dp)
            .padding(0.dp),
        roundedCornersSize = 12.dp,
        borderWidth = 0.dp
    )
}
