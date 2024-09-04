package com.flexa.scan

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toRectF
import com.flexa.core.theme.FlexaTheme

@Composable
fun ScanScreen(
    modifier: Modifier = Modifier,
    close: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var points by remember { mutableStateOf(RectF(0f, 0f, 0f, 0f)) }
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
                    holder.setCodes(it) // debug code
                },
                onBitmap = {
                    holder.setImage(it) // for testing points of interests
                })
        } else {
            Spacer(modifier = Modifier.fillMaxSize())
        }
        Column(
            modifier = Modifier
                .padding(top = 48.dp, bottom = 100.dp)
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val previewMode = LocalInspectionMode.current
            var visibility by remember {
                mutableStateOf(previewMode)
            }
            val firstLaunch = remember { mutableStateOf(false) }
            LaunchedEffect(firstLaunch.value) {
                if (!firstLaunch.value) {
                    firstLaunch.value = true
                    visibility = true
                }
            }
            AnimatedVisibility(
                visible = visibility,
                enter = fadeIn(
                    animationSpec = tween(
                        delayMillis = 300, durationMillis = 300
                    )
                ),
            ) {
                Column(
                    modifier = Modifier
                        .width(200.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(id = R.string.scan_any_code),
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            shadow = Shadow(blurRadius = 3f)
                        ),
                    )
                    Text(
                        stringResource(id = R.string.send_pay_connect),
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            shadow = Shadow(blurRadius = 3f)
                        ),
                    )
                }

            }
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

@Preview(showBackground = true, backgroundColor = 0xFFCECECE)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    showBackground = true, backgroundColor = 0xFF2F2F2F
)
@Composable
fun ScanScreenPreview() {
    FlexaTheme {
        ScanScreen {

        }
    }
}