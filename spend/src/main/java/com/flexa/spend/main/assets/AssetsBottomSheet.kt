package com.flexa.spend.main.assets

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flexa.core.shared.SelectedAsset
import com.flexa.core.theme.FlexaTheme
import com.flexa.core.zeroValue
import com.flexa.spend.R
import com.flexa.spend.Spend
import com.flexa.spend.domain.FakeInteractor
import com.flexa.spend.isSelected
import com.flexa.spend.rememberSelectedAsset

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AssetsBottomSheet(
    viewModel: AssetsViewModel,
    toUrl: (@ParameterName("url") String) -> Unit,
    toBack: () -> Unit
) {
    val previewMode = LocalInspectionMode.current
    val assetsScreen by if (!previewMode) viewModel.assetsScreen.collectAsStateWithLifecycle()
    else remember { mutableStateOf(AssetsScreen.Assets) }
    val appAccounts = viewModel.appAccounts
    val listItems by remember {
        derivedStateOf {
            appAccounts.map {
                val sortedAssets =
                    it.availableAssets.sortedBy { asset -> asset.zeroValue() }
                        .toCollection(ArrayList())
                it.copy(availableAssets = sortedAssets)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedContent( // Header
            targetState = assetsScreen,
            transitionSpec = {
                if (assetsScreen is AssetsScreen.Assets) {
                    (slideInVertically { height -> height } + fadeIn()).togetherWith(
                        slideOutVertically { height -> -height } + fadeOut())
                } else {
                    (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                        slideOutVertically { height -> height } + fadeOut())
                }.using(SizeTransform(clip = false))
            }, label = "header_animation"
        ) { screen ->
            when (screen) {
                is AssetsScreen.Assets -> {
                    AssetsSheetHeader(
                        stringResource(id = R.string.pay_using)
                    ) { viewModel.assetsScreen.value = AssetsScreen.Settings() }
                }

                is AssetsScreen.AssetDetails -> {
                    AssetInfoSheetHeader(
                        backNavigation = true,
                        asset = screen.asset.asset,
                        toBack = {
                            viewModel.assetsScreen.value = AssetsScreen.Assets
                        },
                        toSettings = {
                            viewModel.assetsScreen.value = AssetsScreen.Settings()
                        }
                    )
                }

                is AssetsScreen.Settings -> {
                    AssetsSettingsSheetHeader(
                        toBack = {
                            viewModel.assetsScreen.value = AssetsScreen.Assets
                        }
                    )
                }
            }
        }
        val animDuration by remember { mutableIntStateOf(700) }
        val filtered by viewModel.filtered.collectAsStateWithLifecycle()

        LaunchedEffect(filtered) {
            viewModel.filterValue = 0.0
        }

        AnimatedContent(
            targetState = assetsScreen,
            transitionSpec = {
                if (this.targetState !is AssetsScreen.Assets) {
                    slideInHorizontally(tween(300, easing = EaseOut)) { it } togetherWith
                            slideOutHorizontally(tween(300, easing = LinearEasing)) { -it / 2 }
                } else {
                    slideInHorizontally(tween(300, easing = EaseOut)) { -it } togetherWith
                            slideOutHorizontally(tween(300, easing = LinearEasing)) { it / 2 }
                }.using(
                    SizeTransform(clip = false)
                )
            }, label = ""
        ) { screen ->
            when (screen) {
                is AssetsScreen.Assets -> {
                    val noAssets by remember { derivedStateOf { listItems.sumOf { it.availableAssets.size } == 0 } }
                    Crossfade(targetState = noAssets, label = "") { na ->
                        if (!na) {
                            val selectedAsset by rememberSelectedAsset()
                            val listState = rememberLazyListState()
                            LazyColumn(
                                modifier = Modifier.background(BottomSheetDefaults.ContainerColor),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                state = listState
                            ) {
                                listItems.forEach { appAccount ->
                                    val accountId = appAccount.accountId
                                    if (listItems.size > 1) { // StickyHeader
                                        stickyHeader(key = accountId) {
                                            appAccount.displayName?.let {
                                                AssetsSheetStickyHeader(
                                                    modifier = Modifier,
                                                    text = it,
                                                )
                                            }
                                        }
                                    } else {
                                        stickyHeader(key = accountId) {
                                            Spacer(modifier = Modifier.height(20.dp))
                                        }
                                    }
                                    appAccount.availableAssets.forEachIndexed { index, asset ->
                                        item(key = accountId + asset.assetId) {
                                            val size by remember { mutableIntStateOf(appAccount.availableAssets.size) }
                                            val shape by remember {
                                                mutableStateOf(
                                                    when {
                                                        size == 1 -> RoundedCornerShape(24.dp)
                                                        index == 0 && size > 1 -> RoundedCornerShape(
                                                            topStart = 24.dp,
                                                            topEnd = 24.dp,
                                                            bottomStart = 4.dp,
                                                            bottomEnd = 4.dp
                                                        )

                                                        index == size - 1 -> RoundedCornerShape(
                                                            topStart = 4.dp,
                                                            topEnd = 4.dp,
                                                            bottomStart = 24.dp,
                                                            bottomEnd = 24.dp
                                                        )

                                                        else -> RoundedCornerShape(4)
                                                    }
                                                )
                                            }

                                            val selected by remember(selectedAsset) {
                                                if (!previewMode)
                                                    mutableStateOf(
                                                        selectedAsset?.isSelected(
                                                            appAccount.accountId,
                                                            asset.assetId
                                                        ) == true
                                                    )
                                                else mutableStateOf(index == 0)
                                            }
                                            val color by animateColorAsState(
                                                targetValue = if (selected)
                                                    MaterialTheme.colorScheme.tertiaryContainer
                                                else MaterialTheme.colorScheme.background,
                                                label = "item_color",
                                            )
                                            AssetItemCompose(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .alpha(if (!asset.zeroValue()) 1F else .5F)
                                                    .padding(horizontal = 20.dp)
                                                    .clip(shape)
                                                    .background(color)
                                                    .animateItem(
                                                        placementSpec = tween(durationMillis = animDuration)
                                                    )
                                                    .clickable(enabled = !asset.zeroValue()) {
                                                        viewModel.setSelectedAsset(accountId, asset)
                                                        toBack()
                                                    },
                                                color = color,
                                                asset = asset,
                                                toDetails = { ast ->
                                                    viewModel.assetsScreen.value =
                                                        AssetsScreen.AssetDetails(
                                                            SelectedAsset(accountId, ast)
                                                        )
                                                }
                                            )
                                        }
                                        if (index + 1 < appAccount.availableAssets.size) {
                                            item(key = accountId + "divider" + asset.assetId) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                            }
                                        }
                                    }
                                    item(key = accountId + "spacer") {
                                        Spacer(
                                            modifier = Modifier
                                                .height(24.dp)
                                                .fillMaxWidth()
                                                .animateItem(
                                                    placementSpec = tween(
                                                        durationMillis = animDuration
                                                    )
                                                )
                                        )
                                    }

                                }
                                item(key = "footer_spacer") {
                                    Spacer(modifier = Modifier.height(120.dp))
                                    Spacer(modifier = Modifier.navigationBarsPadding())
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .height(300.dp)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.TopCenter,
                            ) {
                                Text(
                                    text = "   " + stringResource(id = R.string.refreshing_assets),
                                    style = TextStyle(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.W600
                                    )
                                )
                            }
                        }
                    }
                }

                is AssetsScreen.AssetDetails -> {
                    val context = LocalContext.current
                    AssetDetailsScreen(
                        modifier = Modifier.fillMaxSize(),
                        viewModel = viewModel(
                            initializer = {
                                AssetDetailViewModel(Spend.interactor)
                            }),
                        assetBundle = screen.asset,
                        assetsViewModel = viewModel,
                        toLearnMore = { toUrl(context.getString(R.string.learn_more_link)) },
                    )
                }

                is AssetsScreen.Settings -> {
                    val context = LocalContext.current
                    AssetsSettingsScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BottomSheetDefaults.ContainerColor),
                        filtered = filtered,
                        onFiltered = { viewModel.filtered.value = it },
                        toLearnMore = { toUrl(context.getString(R.string.learn_more_link)) }
                    )
                }
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun AssetsBottomSheetContentPreview() {
    FlexaTheme {
        Surface {
            AssetsBottomSheet(
                viewModel = AssetsViewModel(interactor = FakeInteractor()),
                toUrl = {},
                toBack = {}
            )
        }
    }
}
