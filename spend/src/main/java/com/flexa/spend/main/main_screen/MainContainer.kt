package com.flexa.spend.main.main_screen

import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flexa.core.entity.Notification
import com.flexa.core.shared.SelectedAsset
import com.flexa.core.theme.FlexaTheme
import com.flexa.core.zeroValue
import com.flexa.spend.MockFactory
import com.flexa.spend.R
import com.flexa.spend.domain.FakeInteractor
import com.flexa.spend.main.NoAssetsCard
import com.flexa.spend.main.assets.AssetsState
import com.flexa.spend.main.assets.AssetsViewModel
import com.flexa.spend.main.ui_utils.rememberSelectedAsset
import com.flexa.spend.merchants.BrandsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.absoluteValue

@Composable
fun Spend(
    modifier: Modifier = Modifier,
    viewModel: SpendViewModel,
    assetsViewModel: AssetsViewModel,
    brandsViewModel: BrandsViewModel,
    sheetStateVisible: Boolean,
    toAssets: () -> Unit,
    toAddAssets: () -> Unit,
    toAssetInfo: (SelectedAsset) -> Unit,
    toEdit: () -> Unit,
    toInputAmount: () -> Unit,
    toUrl: (@ParameterName("url") String) -> Unit,
) {

    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        val previewMode = LocalInspectionMode.current
        val assetsState by if (!previewMode) assetsViewModel.assetsState.collectAsStateWithLifecycle() else MutableStateFlow(
            AssetsState.Fine(emptyList())
        ).collectAsState()
        val hasAssets by remember {
            derivedStateOf { assetsState !is AssetsState.NoAssets }
        }

        AnimatedVisibility(
            visible = hasAssets,
            exit = fadeOut(tween(500)) + shrinkVertically(tween(500))
        ) {
            Column {
                val selectedAsset by rememberSelectedAsset()
                FilledTonalButton(// Assets Button
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 16.dp),
                    onClick = { toAssets.invoke() },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    val angle by animateFloatAsState(
                        targetValue = if (sheetStateVisible
                            && viewModel.sheetScreen is SheetScreen.Assets
                        ) -180F else 0F, label = "assets button angle"
                    )
                    Text(
                        modifier = Modifier
                            .animateContentSize(tween(500)),
                        text = "${stringResource(id = R.string.using)} ${
                            selectedAsset?.asset?.assetData?.displayName ?: ""
                        }",
                        style = TextStyle(
                            fontWeight = FontWeight.W600,
                            fontSize = 17.sp,
                        ),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(angle),
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                val assets by remember {
                    derivedStateOf {
                        if (!previewMode) {
                            assetsViewModel.assets.filter { !it.asset.zeroValue() }
                        } else {
                            listOf(MockFactory.getMockSelectedAsset())
                        }
                    }
                }

                val pagerState = rememberPagerState(
                    initialPage = assets.indexOfFirst {
                        it.accountId == selectedAsset?.accountId &&
                                it.asset.assetId == selectedAsset?.asset?.assetId
                    }.coerceAtLeast(0),
                    pageCount = { assets.size.coerceAtLeast(1) }
                )

                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.currentPage }
                        .collect { page ->
                            assets.getOrNull(page)?.let {
                                assetsViewModel.setSelectedAsset(it.accountId, it.asset)
                            }
                        }
                }

                LaunchedEffect(selectedAsset, assets) {
                    selectedAsset?.let { sa ->
                        val index = assets.indexOfFirst {
                            it.accountId == sa.accountId && it.asset.assetId == sa.asset.assetId
                        }
                        if (index != -1 && index != pagerState.currentPage)
                            pagerState.scrollToPage(index)
                    }
                }

                // Flexcodes
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    pageSpacing = 4.dp,
                ) { page ->
                    val asset by remember { derivedStateOf { assets.getOrNull(page) } }
                    FlexcodePagerCard(
                        pagerState = pagerState,
                        page = page,
                        assetsSize = assets.size,
                        asset = asset,
                        viewModel = assetsViewModel,
                        toAssetInfo = {
                            Log.d(null, "Spend: toAssetInfo root: $selectedAsset")
                            asset?.let { toAssetInfo.invoke(it) }
                        }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = assetsState is AssetsState.NoAssets,
            enter = fadeIn(tween(500)) + expandVertically(tween(500))
        ) { // No Assets Card
            NoAssetsCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(470.dp)
                    .padding(horizontal = 16.dp),
                assetsState = assetsState,
                toAddAssets = { toAddAssets() }
            )
        }

        val notifications = viewModel.notifications
        val showNotifications by remember {
            derivedStateOf { notifications.isNotEmpty() }
        }
        Spacer(modifier = Modifier.height(10.dp))
        AnimatedContent(
            targetState = showNotifications,
            transitionSpec = {
                (expandVertically() + fadeIn()).togetherWith(
                    shrinkVertically() + fadeOut()
                )
            }, label = "notifications"
        ) { state ->
            if (!state) {
                Spacer(modifier = Modifier.height(10.dp))
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val scope = rememberCoroutineScope()
                    val pagerState =
                        rememberPagerState(pageCount = { notifications.size }, initialPage = 0)
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalPager(
                        state = pagerState,
                        key = { notifications[it].id ?: UUID.randomUUID().toString() }
                    ) { page ->
                        val item: Notification = notifications[page]
                        var close by remember { mutableStateOf(false) }
                        AppNotification(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(24.dp)),
                            appNotification = item,
                            toUrl = { url -> toUrl(url) },
                            onClose = { n ->
                                if (notifications.size == 1) {
                                    viewModel.removeNotification(notifications[0])
                                } else {
                                    scope.launch {
                                        val nextPage = if (notifications.size == page + 1)
                                            page - 1 else page + 1
                                        pagerState.animateScrollToPage(
                                            page = nextPage,
                                            animationSpec = tween(500)
                                        )
                                        close = true
                                        viewModel.removeNotification(n)
                                    }
                                }
                            }
                        )
                    }
                    AnimatedVisibility(notifications.size == 1) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    AnimatedVisibility(notifications.size > 1) {
                        val palette = MaterialTheme.colorScheme
                        LazyRow(
                            modifier = Modifier
                                .height(14.dp)
                                .padding(top = 10.dp),
                            userScrollEnabled = false,
                        ) {
                            repeat(pagerState.pageCount) { page ->
                                item(key = notifications[page].id) {
                                    Canvas(
                                        modifier = Modifier
                                            .width(30.dp)
                                            .animateItem()
                                            .graphicsLayer {
                                                val pageOffset =
                                                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                                alpha =
                                                    (1.3F - pageOffset.absoluteValue).coerceAtLeast(
                                                        .3F
                                                    )
                                                scaleX =
                                                    (1.3F - pageOffset.absoluteValue / 5).coerceAtLeast(
                                                        1.0F
                                                    )
                                                scaleY =
                                                    (1.3F - pageOffset.absoluteValue / 5).coerceAtLeast(
                                                        1.0F
                                                    )
                                            }
                                    ) { drawCircle(color = palette.primary, radius = 3.dp.toPx()) }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        AnimatedVisibility(visible = hasAssets) {
            BrandsRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(color = MaterialTheme.colorScheme.onPrimary),
                viewModel = brandsViewModel,
                toEdit = { toEdit.invoke() },
                onClick = { brand ->
                    viewModel.selectedBrand.value = brand
                    toInputAmount()
                }
            )
        }
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun PayPreview() {
    FlexaTheme {
        Spend(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            viewModel = viewModel(initializer = {
                SpendViewModel(FakeInteractor())
            }),
            sheetStateVisible = false,
            brandsViewModel = viewModel(initializer = {
                BrandsViewModel(FakeInteractor())
            }),
            assetsViewModel = viewModel(initializer = {
                AssetsViewModel(FakeInteractor())
            }),
            toAssets = {},
            toAddAssets = {},
            toAssetInfo = {},
            toEdit = {},
            toUrl = {},
            toInputAmount = {}
        )
    }
}
