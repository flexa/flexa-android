package com.flexa.spend.main.assets

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flexa.core.shared.SelectedAsset
import com.flexa.core.theme.FlexaTheme
import com.flexa.spend.MockFactory
import com.flexa.spend.R
import com.flexa.spend.domain.FakeInteractor

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun AssetDetailsScreen(
    modifier: Modifier = Modifier,
    viewModel: AssetDetailViewModel,
    assetsViewModel: AssetsViewModel,
    assetBundle: SelectedAsset,
    color: Color = MaterialTheme.colorScheme.primaryContainer,
    toLearnMore: () -> Unit
) {

    val density = LocalDensity.current
    var height by remember { mutableStateOf(200.dp) }
    val error by viewModel.error.collectAsStateWithLifecycle()

    val asset by remember {
        derivedStateOf {
            assetsViewModel.assets.firstOrNull {
                it.accountId == assetBundle.accountId && it.asset.assetId == assetBundle.asset.assetId
            }
        }
    }

    LaunchedEffect(asset) {
        asset?.let {
            viewModel.asset = it
            viewModel.getQuote()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clear()
        }
    }

    Column(
        modifier = modifier.background(color)
    ) {
        val palette = MaterialTheme.colorScheme
        AnimatedContent(
            targetState = error,
            transitionSpec = {
                scaleIn(
                    initialScale = 1.2F,
                    animationSpec = tween(700)
                ) + fadeIn(animationSpec = tween(700)) with
                        scaleOut(targetScale = .8F, animationSpec = tween(700)) + fadeOut(
                    animationSpec = tween(700)
                )
            },
            label = "error card"
        ) { e ->
            if (!e) {
                Column(
                    modifier = Modifier
                        .onGloballyPositioned {
                            height = with(density) { it.size.height.toDp() }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val quote by viewModel.quote.collectAsStateWithLifecycle()
                    val progress by viewModel.progress.collectAsStateWithLifecycle()
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = color),
                        leadingContent = {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                imageVector = Icons.Outlined.AccountBalanceWallet,
                                contentDescription = null
                            )
                        },
                        headlineContent = {
                            AnimatedContent(
                                targetState = asset?.asset?.value?.labelTitlecase,
                                transitionSpec = {
                                    if ((targetState?.length ?: 0) < (initialState?.length ?: 0)) {
                                        slideInVertically { width -> width } +
                                                fadeIn() with slideOutVertically()
                                        { width -> -width } + fadeOut()
                                    } else {
                                        slideInVertically { width -> -width } +
                                                fadeIn() with slideOutVertically()
                                        { width -> width } + fadeOut()
                                    }.using(SizeTransform(clip = false))
                                }, label = ""
                            ) { state ->
                                state
                                Text(
                                    text = asset?.asset?.value?.labelTitlecase ?: "",
                                    style = TextStyle(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.W400,
                                        color = palette.onBackground
                                    )
                                )
                            }
                        },
                        supportingContent = {
                            Text(
                                text = asset?.asset?.label ?: "",
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.W400,
                                    color = palette.outline
                                )
                            )
                        }
                    )
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = color),
                        leadingContent = {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                imageVector = Icons.Outlined.SwapHoriz,
                                contentDescription = null
                            )
                        },
                        headlineContent = {
                            AnimatedContent(
                                targetState = quote?.value?.rate?.label,
                                transitionSpec = {
                                    if ((targetState?.length ?: 0) < (initialState?.length ?: 0)) {
                                        slideInVertically { width -> width } +
                                                fadeIn() with slideOutVertically()
                                        { width -> -width } + fadeOut()
                                    } else {
                                        slideInVertically { width -> -width } +
                                                fadeIn() with slideOutVertically()
                                        { width -> width } + fadeOut()
                                    }.using(SizeTransform(clip = false))
                                }, label = ""
                            ) {
                                Text(
                                    text = it
                                        ?: "${stringResource(R.string.updating)}...",
                                    style = TextStyle(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.W400,
                                        color = palette.onBackground
                                    )
                                )

                            }
                        }
                    )
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = color),
                        leadingContent = {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                imageVector = Icons.Outlined.Language,
                                contentDescription = null
                            )
                        },
                        headlineContent = {
                            AnimatedContent(
                                targetState = quote?.fee?.equivalent,
                                transitionSpec = {
                                    if ((targetState?.length ?: 0) < (initialState?.length ?: 0)) {
                                        slideInVertically { width -> width } +
                                                fadeIn() with slideOutVertically()
                                        { width -> -width } + fadeOut()
                                    } else {
                                        slideInVertically { width -> -width } +
                                                fadeIn() with slideOutVertically()
                                        { width -> width } + fadeOut()
                                    }.using(SizeTransform(clip = false))
                                }, label = ""
                            ) { state ->
                                state
                                Text(
                                    text = quote?.fee?.equivalent ?: "",
                                    style = TextStyle(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.W400,
                                        color = palette.onBackground
                                    )
                                )
                            }
                        },
                        supportingContent = {
                            Text(
                                text = if (progress)
                                    "${stringResource(id = R.string.updating)}..."
                                else
                                    stringResource(id = R.string.network_fee),
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.W400,
                                    color = palette.outline
                                )
                            )
                        }
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        modifier = Modifier.size(36.dp),
                        imageVector = Icons.Rounded.WarningAmber,
                        tint = MaterialTheme.colorScheme.error,
                        contentDescription = null
                    )
                    Text(
                        "Can't retrieve ${assetBundle.asset.assetData?.displayName} quote!",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = { viewModel.getQuote() }
                    ) { Text(stringResource(R.string.retry).uppercase()) }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Divider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = palette.outline.copy(alpha = .5F),
            thickness = 1.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        AssetInfoFooter(
            modifier = Modifier.padding(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 32.dp
            )
        ) { toLearnMore() }
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
fun AssetInfoFooter(
    modifier: Modifier = Modifier,
    toLearnMore: () -> Unit
) {
    val bestExchangeDescription = stringResource(id = R.string.best_exchange_rate)
    val palette = MaterialTheme.colorScheme
    Row(modifier = modifier) {
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            modifier = Modifier
                .size(14.dp)
                .offset(y = 2.5.dp),
            imageVector = Icons.Outlined.Info,
            tint = palette.outline,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(12.dp))
        ClickableText(
            text = buildAnnotatedString {
                append(
                    AnnotatedString(
                        text = bestExchangeDescription
                    )
                )
                append(AnnotatedString(" "))
                append(
                    AnnotatedString(
                        stringResource(id = R.string.learn_more),
                        spanStyle = SpanStyle(textDecoration = TextDecoration.Underline)
                    )
                )
            },
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.W400,
                color = palette.outline
            ),
            onClick = { position ->
                if (position > bestExchangeDescription.length) {
                    toLearnMore.invoke()
                }
            }
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AssetDetailContentPreview() {
    FlexaTheme {
        AssetDetailsScreen(
            viewModel = AssetDetailViewModel(
                FakeInteractor()
            ).apply {
                quote.value = MockFactory.getMockQuote()
            },
            assetBundle = MockFactory.getMockSelectedAsset(),
            assetsViewModel = AssetsViewModel(FakeInteractor()),
            toLearnMore = {}
        )
    }
}
