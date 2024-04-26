package com.flexa.scan

import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toRectF
import com.flexa.core.theme.FlexaTheme

@Composable
fun ScanScreen(
    modifier: Modifier = Modifier,
    close: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var points by remember { mutableStateOf<RectF?>(null) }
    val holder by remember { mutableStateOf(Holder(scope)) }

    Box(
        modifier = modifier
    ) {
        if (!LocalInspectionMode.current) {
            KeepScreenOn()
            FlexaScanner(
                modifier = Modifier
                    .fillMaxSize(),
                points,
                onQrCode = {
                    //holder.setCodes(it) // debug code
                },
                onBitmap = {
                    holder.setImage(it) // for testing points of interests
                })
        } else {
            Spacer(modifier = Modifier.fillMaxSize())
        }
/*
        Row(// Toolbar
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .height(48.dp)
                .padding(4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val context = LocalContext.current
            IconButton(
                onClick = {  }) {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp),
                        onDraw = {
                            drawCircle(color = Color.DarkGray.copy(alpha = .5f))
                        })
                    Icon(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxSize()
                            .padding(10.dp),
                        imageVector = Icons.Rounded.MoreHoriz,
                        tint = Color.White,
                        contentDescription = "Settings"
                    )
                }
            }
            IconButton(
                onClick = { close() }) {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp),
                        onDraw = {
                            drawCircle(color = Color.DarkGray.copy(alpha = .5f))
                        })
                    Icon(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxSize()
                            .padding(10.dp),
                        imageVector = Icons.Rounded.Clear,
                        tint = Color.White,
                        contentDescription = "Close"
                    )
                }

            }
        }
*/
        Column(
            modifier = Modifier
                .padding(top = 48.dp, bottom = 100.dp)
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            var visibility by remember {
                mutableStateOf(false)
            }
            AnimatedVisibility(
                visible = visibility,
                enter = fadeIn(
                    animationSpec = tween(
                        delayMillis = 1000, durationMillis = 300
                    )
                ),
            ) {
                TopContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1.5f)
                )
            }
            Box(// center
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { visibility = true }
            )
            Box(// bottom
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                Codes(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    holder
                )
            }
        }
        ScannerBox(modifier = Modifier
            .align(Alignment.Center)
            .aspectRatio(1f)
            .fillMaxWidth()
            .padding(56.dp),
            onGloballyPositioned = {
                val size = it.size
                val position = it.positionInRoot()
                val coordinates = Rect(
                    position.x.toInt(),
                    position.y.toInt(),
                    position.x.toInt() + size.width,
                    position.y.toInt() + size.height
                )
                points = coordinates.toRectF()
            }
        )
    }
}

@Preview
@Composable
fun ScanScreenPreview() {
    FlexaTheme {
        ScanScreen {

        }
    }
}