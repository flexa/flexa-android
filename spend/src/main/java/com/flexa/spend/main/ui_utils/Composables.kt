package com.flexa.spend.main.ui_utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.decode.BitmapFactoryDecoder
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.flexa.core.shared.SelectedAsset
import com.flexa.spend.MockFactory
import com.flexa.spend.R
import com.flexa.spend.Spend

@Composable
internal fun SpendAsyncImage(
    modifier: Modifier,
    imageUrl: String?,
    crossfade: Boolean = true,
    crossfadeDuration: Int = 300,
    placeholder: Painter? = null,
) {
    AsyncImage(
        modifier = modifier,
        model = ImageRequest.Builder(LocalContext.current)
            .decoderFactory(BitmapFactoryDecoder.Factory())
            .decoderFactory(SvgDecoder.Factory())
            .data(imageUrl)
            .crossfade(crossfade)
            .crossfade(crossfadeDuration)
            .build(),
        contentDescription = null,
        placeholder = placeholder,
        error = painterResource(id = R.drawable.ic_flexa)
    )
}

@Composable
fun rememberSelectedAsset(): State<SelectedAsset?> {
    val previewMode = LocalInspectionMode.current
    return if (!previewMode) Spend.selectedAsset.collectAsStateWithLifecycle()
    else remember { mutableStateOf(MockFactory.getMockSelectedAsset()) }
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
