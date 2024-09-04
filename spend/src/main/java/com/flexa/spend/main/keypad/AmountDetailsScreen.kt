package com.flexa.spend.main.keypad

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flexa.core.shared.SelectedAsset
import com.flexa.core.theme.FlexaTheme
import com.flexa.spend.MockFactory
import com.flexa.spend.R
import com.flexa.spend.domain.FakeInteractor
import com.flexa.spend.main.assets.AssetInfoFooter
import com.flexa.spend.main.assets.AssetsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AmountDetailsScreen(
    modifier: Modifier = Modifier,
    assetBundle: SelectedAsset,
    amount: String,
    viewModel: AmountDetailViewModel,
    assetsViewModel: AssetsViewModel,
    toLearnMore: () -> Unit,
) {
    val color = BottomSheetDefaults.ContainerColor
    Column(modifier = modifier.background(color)) {
        val asset by remember {
            derivedStateOf {
                assetsViewModel.assets.firstOrNull {
                    it.accountId == assetBundle.accountId && it.asset.assetId == assetBundle.asset.assetId
                }
            }
        }
        val palette = MaterialTheme.colorScheme
        val quote by viewModel.quote.collectAsStateWithLifecycle()
        val progress by viewModel.progress.collectAsStateWithLifecycle()
        val error by viewModel.error.collectAsStateWithLifecycle()

        LaunchedEffect(asset) {
            asset?.let {
                viewModel.assetAmount = AssetAndAmount(it, amount)
                viewModel.getQuote()
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                viewModel.percentJob?.cancel()
            }
        }
        Text(
            modifier = Modifier
                .padding(start = 16.dp),
            text = assetBundle.asset.assetData?.displayName ?: "",
            style = TextStyle(
                color = palette.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        )

        var height by remember { mutableStateOf(200.dp) }
        val density = LocalDensity.current
        AnimatedContent(
            targetState = error,
            transitionSpec = {
                (scaleIn(
                    initialScale = 1.2F,
                    animationSpec = tween(700)
                ) + fadeIn(animationSpec = tween(700))).togetherWith(
                    scaleOut(targetScale = .8F, animationSpec = tween(700)) + fadeOut(
                        animationSpec = tween(700)
                    )
                )
            },
            label = "error card"
        ) { e ->
            if (!e) {
                Column(
                    modifier = Modifier.onGloballyPositioned {
                        height = with(density) { it.size.height.toDp() }
                    }
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(
                            containerColor = color
                        ),
                        leadingContent = {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                imageVector = Icons.Outlined.AccountBalanceWallet,
                                contentDescription = null
                            )
                        },
                        headlineContent = {
                            AnimatedContent(
                                targetState = quote?.value?.label,
                                transitionSpec = {
                                    if ((targetState?.length ?: 0) < (initialState?.length ?: 0)) {
                                        (slideInVertically { width -> width } +
                                                fadeIn()).togetherWith(slideOutVertically()
                                        { width -> -width } + fadeOut())
                                    } else {
                                        (slideInVertically { width -> -width } +
                                                fadeIn()).togetherWith(slideOutVertically()
                                        { width -> width } + fadeOut())
                                    }.using(SizeTransform(clip = false))
                                }, label = ""
                            ) { state ->
                                Text(
                                    text = state ?: "",
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
                                text = quote?.label ?: "",
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
                                    if ((targetState?.length ?: 0) < (initialState?.length
                                            ?: 0)
                                    ) {
                                        (slideInVertically { width -> width } +
                                                fadeIn()).togetherWith(slideOutVertically()
                                        { width -> -width } + fadeOut())
                                    } else {
                                        (slideInVertically { width -> -width } +
                                                fadeIn()).togetherWith(slideOutVertically()
                                        { width -> width } + fadeOut())
                                    }.using(SizeTransform(clip = false))
                                }, label = ""
                            ) { label ->
                                Text(
                                    text = if (!progress) label
                                        ?: "" else "${stringResource(R.string.updating)}...",
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
                                        (slideInVertically { width -> width } +
                                                fadeIn()).togetherWith(slideOutVertically()
                                        { width -> -width } + fadeOut())
                                    } else {
                                        (slideInVertically { width -> -width } +
                                                fadeIn()).togetherWith(slideOutVertically()
                                        { width -> width } + fadeOut())
                                    }.using(SizeTransform(clip = false))
                                }, label = ""
                            ) { state ->
                                Text(
                                    text = state ?: "",
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
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 1.dp,
            color = palette.outline.copy(alpha = .5F)
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

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun AmountDetailsPreview() {
    FlexaTheme {
        AmountDetailsScreen(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(color = MaterialTheme.colorScheme.background),
            assetBundle = MockFactory.getMockSelectedAsset(),
            viewModel = AmountDetailViewModel(FakeInteractor()).apply {
                quote.value = MockFactory.getMockQuote()
            },
            assetsViewModel = AssetsViewModel(FakeInteractor()),
            amount = "5.23",
            toLearnMore = {},
        )
    }
}