package com.flexa.spend.main.main_screen

import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flexa.core.shared.SelectedAsset
import com.flexa.core.theme.FlexaTheme
import com.flexa.spend.BuildConfig
import com.flexa.spend.MockFactory
import com.flexa.spend.R
import com.flexa.spend.domain.FakeInteractor
import com.flexa.spend.main.flexcode.FlexcodeLayout
import com.flexa.spend.rememberTOTP
import com.flexa.spend.toColor
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Instant

@Composable
fun FlexcodePagerCard(
    pagerState: PagerState,
    page: Int,
    assetsSize: Int,
    asset: SelectedAsset?,
    viewModel: SpendViewModel,
    toAssetInfo: (SelectedAsset) -> Unit
) {
    val radiusStart by remember(page, assetsSize) {
        mutableStateOf(
            when {
                page == 0 -> 24.dp
                assetsSize > 1 -> 8.dp
                else -> 24.dp
            }
        )
    }
    val radiusEnd by remember(page, assetsSize) {
        mutableStateOf(
            when {
                page + 1 == assetsSize -> 24.dp
                assetsSize > 1 -> 8.dp
                else -> 24.dp
            }
        )
    }
    Box(
        modifier = Modifier
            .clip(
                RoundedCornerShape(
                    topStart = radiusStart,
                    bottomStart = radiusStart,
                    topEnd = radiusEnd,
                    bottomEnd = radiusEnd
                )
            )
            .background(color = MaterialTheme.colorScheme.onPrimary)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .aspectRatio(1f)
                .padding(
                    top = 32.dp,
                    start = 32.dp,
                    end = 32.dp,
                    bottom = 28.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .graphicsLayer {
                        val pageOffset = (
                                (pagerState.currentPage - page) + pagerState
                                    .currentPageOffsetFraction
                                )
                        translationX = lerp(
                            start = 0f,
                            stop = -300f,
                            fraction = pageOffset.coerceIn(-1f, 1f),
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                val assetKey by remember(asset) {
                    mutableStateOf(asset?.asset?.key)
                }
                val totpGenerator by rememberTOTP(
                    assetKey?.secret
                        ?: BuildConfig.LIBRARY_PACKAGE_NAME,
                    assetKey?.length ?: 1
                )
                val duration by viewModel.duration.collectAsStateWithLifecycle()
                val code = remember {
                    mutableStateOf("${assetKey?.prefix ?: ""}${totpGenerator.generate()}")
                }
                val codeProgress = remember { mutableStateOf(false) }
                LaunchedEffect(assetKey, duration) {
                    if (assetKey != null) {
                        while (isActive) {
                            val date =
                                Instant.now().minusMillis(duration.toMillis())
                            val oldCode = code.value
                            val totp = totpGenerator.generate(date)
                            val newCode = "${assetKey?.prefix ?: ""}$totp"
                            if (newCode != oldCode) {
                                codeProgress.value = true
                            }
                            code.value = newCode
                            Log.d(
                                "TAG",
                                "Spend: newCode [${code.value}] date: $date [${date.toEpochMilli()}] prefix: ${assetKey?.prefix} code: $totp secret: ${assetKey?.secret} asset: ${asset?.asset?.label} ${asset?.asset?.assetData?.displayName}"
                            )
                            val counter = totpGenerator.counter(date)
                            val endEpochMillis =
                                totpGenerator.timeslotStart(counter + 1) - 1
                            val millisValid =
                                endEpochMillis - date.toEpochMilli()
                            val a = async {
                                delay(300)
                                codeProgress.value = false
                            }
                            delay(millisValid)
                            a.await()
                        }
                    }
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    val blur by animateDpAsState(
                        if (codeProgress.value) 2.dp else 0.dp, label = "blur"
                    )
                    FlexcodeLayout(
                        modifier = Modifier
                            .fillMaxSize()
                            .aspectRatio(1.14f)
                            .blur(blur, BlurredEdgeTreatment.Rectangle),
                        code = code.value,
                        color = asset?.asset?.assetData?.color?.toColor() ?: Color.Magenta
                    )
                } else {
                    Crossfade(
                        targetState = code.value, label = "Flexcode",
                        animationSpec = tween(1000)
                    ) { code ->
                        FlexcodeLayout(
                            modifier = Modifier
                                .aspectRatio(1.14f)
                                .fillMaxSize(),
                            code = code,
                            color = asset?.asset?.assetData?.color?.toColor() ?: Color.Magenta
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            FlexcodeButton(
                modifier = Modifier
                    .widthIn(min = 140.dp)
                    .offset(y = 10.dp)
                    .graphicsLayer {
                        val pageOffset = (
                                (pagerState.currentPage - page) + pagerState
                                    .currentPageOffsetFraction
                                )
                        translationX = lerp(
                            start = 0f,
                            stop = -500f,
                            fraction = pageOffset.coerceIn(-1f, 1f),
                        )
                    }
                    .animateContentSize(),
                price = asset?.asset?.value?.labelTitlecase
                    ?: stringResource(R.string.updating)
            ) { asset?.let(toAssetInfo) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
fun FlexcodePagerCardPreview() {
    FlexaTheme {
        FlexcodePagerCard(
            pagerState = rememberPagerState(
                initialPage = 0, pageCount = { 1 }
            ),
            page = 0,
            assetsSize = 1,
            asset = MockFactory.getMockSelectedAsset(),
            viewModel = SpendViewModel(FakeInteractor()),
            toAssetInfo = {}
        )
    }
}