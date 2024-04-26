package com.flexa.spend.main.main_screen

import android.app.Activity
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flexa.core.shared.ErrorDialog
import com.flexa.core.theme.FlexaTheme
import com.flexa.spend.MockFactory
import com.flexa.spend.R
import com.flexa.spend.Spend
import com.flexa.spend.domain.CommerceSessionWorker
import com.flexa.spend.domain.FakeInteractor
import com.flexa.spend.isNexGen
import com.flexa.spend.limits_and_features.LimitsAndFeaturesScreen
import com.flexa.spend.main.assets.AssetDetailViewModel
import com.flexa.spend.main.assets.AssetDetailsScreen
import com.flexa.spend.main.assets.AssetInfoSheetHeader
import com.flexa.spend.main.assets.AssetsScreen
import com.flexa.spend.main.assets.AssetsState
import com.flexa.spend.main.assets.AssetsViewModel
import com.flexa.spend.main.assets.NavigationDrawer
import com.flexa.spend.main.assets.SpendBottomSheet
import com.flexa.spend.main.confirm.ConfirmCard
import com.flexa.spend.main.confirm.ConfirmViewModel
import com.flexa.spend.main.confirm.PaymentDetailsScreen
import com.flexa.spend.main.keypad.AmountDetailViewModel
import com.flexa.spend.main.keypad.AmountDetailsScreen
import com.flexa.spend.main.keypad.InputAmountScreen
import com.flexa.spend.main.keypad.InputAmountViewModel
import com.flexa.spend.main.legacy_flexcode.LegacyFlexcode
import com.flexa.spend.main.places_to_pay.Locations
import com.flexa.spend.main.places_to_pay.PlacesToPay
import com.flexa.spend.main.places_to_pay.PlacesToPayViewModel
import com.flexa.spend.main.settings_popup.PopupViewModel
import com.flexa.spend.main.settings_popup.SettingsPopup
import com.flexa.spend.merchants.BrandsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalMaterial3Api::class,
)
@Composable
fun SpendScreen(
    modifier: Modifier = Modifier,
    viewModel: SpendViewModel,
    assetsViewModel: AssetsViewModel,
    brandsViewModel: BrandsViewModel,
    settingsPopupViewModel: PopupViewModel = viewModel(),
    toBack: () -> Unit,
    toEdit: () -> Unit,
    toManageAccount: () -> Unit,
    toUrl: (@ParameterName("url") String) -> Unit,
) {
    val skip by remember {
        derivedStateOf { viewModel.sheetScreen !is SheetScreen.Assets }
    }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(modifier = modifier) {
        val sheetState = rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Hidden,
            skipHalfExpanded = skip
        )
        val closeBottomSheet = {
            scope.launch {
                sheetState.hide()
                viewModel.sheetScreen = SheetScreen.Void
            }
        }

        LaunchedEffect(sheetState.isVisible) {
            if (!sheetState.isVisible) viewModel.sheetScreen = SheetScreen.Void
        }

        BackHandler(sheetState.isVisible) {
            closeBottomSheet()
        }

        ModalBottomSheetLayout(
            sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            sheetBackgroundColor = Color.Transparent,
            sheetElevation = 0.dp,
            sheetState = sheetState,
            sheetContent = {
                when (val screen = viewModel.sheetScreen) {
                    is SheetScreen.Void -> {
                        LaunchedEffect(Unit) { sheetState.hide() }
                    }

                    is SheetScreen.Assets ->
                        SpendBottomSheet(
                            viewModel = assetsViewModel,
                        ) { closeBottomSheet() }

                    is SheetScreen.AssetDetails -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                                .background(color = MaterialTheme.colorScheme.background),
                        ) {
                            NavigationDrawer()
                            AssetInfoSheetHeader(
                                asset = screen.asset.asset,
                                toBack = {},
                                toSettings = {}
                            )
                            AssetDetailsScreen(
                                viewModel = viewModel(
                                    initializer = {
                                        AssetDetailViewModel(Spend.interactor)
                                    }),
                                assetBundle = screen.asset,
                                assetsViewModel = assetsViewModel,
                                color = MaterialTheme.colorScheme.background,
                                toLearnMore = {},
                            )
                        }
                    }

                    is SheetScreen.AmountDetails -> {
                        AmountDetailsScreen(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                                .background(color = MaterialTheme.colorScheme.background),
                            viewModel = viewModel(
                                initializer = {
                                    AmountDetailViewModel(Spend.interactor)
                                }),
                            assetsViewModel = assetsViewModel,
                            assetBundle = screen.asset,
                            amount = screen.amount,
                            toLearnMore = {}
                        )
                    }

                    is SheetScreen.LimitsFeatures -> {
                        LimitsAndFeaturesScreen(
                            modifier = Modifier.fillMaxWidth(),
                            toBack = { closeBottomSheet() },
                        )
                    }

                    is SheetScreen.PlacesToPay -> {
                        PlacesToPay(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                            spendViewModel = viewModel,
                            viewModel = viewModel(key = "ptp", initializer = {
                                PlacesToPayViewModel("directory")
                            }),
                            sheetState = sheetState,
                            toBack = { closeBottomSheet() }
                        )
                    }

                    is SheetScreen.Locations -> {
                        Locations(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                            spendViewModel = viewModel,
                            viewModel = viewModel(key = "locations", initializer = {
                                PlacesToPayViewModel("directory/${viewModel.brand.value?.name}/locations")
                            }),
                            sheetState = sheetState,
                            toBack = { closeBottomSheet() }
                        )
                    }

                    is SheetScreen.PaymentDetails -> {
                        val sessionFlow by remember {
                            mutableStateOf(
                                (viewModel.sheetScreen as? SheetScreen.PaymentDetails)?.session
                                    ?: MutableStateFlow(null)
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                                .background(color = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            NavigationDrawer()
                            PaymentDetailsScreen(
                                modifier = Modifier.fillMaxWidth(),
                                sessionFlow = sessionFlow,
                                toBack = { closeBottomSheet() },
                                toLearnMore = {/*TODO*/ }
                            )
                        }
                    }
                }
            }) {
            val isPreview = LocalInspectionMode.current
            val assetsState by if (!isPreview) assetsViewModel.assetsState.collectAsStateWithLifecycle() else MutableStateFlow(
                AssetsState.Fine(emptyList())
            ).collectAsState()
            val isAssetsState by remember { derivedStateOf { assetsState !is AssetsState.NoAssets } }
            val scrollBehavior = if (isAssetsState)
                TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
            else TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
            val openBottomSheet: (SheetScreen) -> (Unit) = {
                val sameScreen = viewModel.sheetScreen == it
                if (sameScreen)
                    scope.launch { sheetState.show() }
                else viewModel.sheetScreen = it
            }
            var firstLaunch by remember { mutableStateOf(true) }

            val commerceSession by viewModel.commerceSession.collectAsStateWithLifecycle()
            val showNextGenFlexcode by remember {
                derivedStateOf { commerceSession.isNexGen() }
            }
            val showLegacyFlexcode by viewModel.openLegacyCard.collectAsStateWithLifecycle()
            val showAmount by viewModel.openAmount.collectAsStateWithLifecycle()

            LaunchedEffect(viewModel.sheetScreen) {
                if (firstLaunch) firstLaunch = false
                else sheetState.show()
            }

            AnimatedVisibility( // Background Dim
                modifier = Modifier.zIndex(.1F),
                visible = showNextGenFlexcode || showLegacyFlexcode || settingsPopupViewModel.opened,
                enter = fadeIn(animationSpec = tween(durationMillis = 500, delayMillis = 0)),
                exit = fadeOut(animationSpec = tween(durationMillis = 500, delayMillis = 0))
            ) {
                Canvas(modifier = Modifier
                    .fillMaxSize(),
                    onDraw = {
                        drawRect(color = Color.Black.copy(alpha = .4F))
                    })
            }

            AnimatedVisibility( // Legacy Flexcode Card
                modifier = Modifier.zIndex(.2F),
                visible = showLegacyFlexcode,
                enter = fadeIn() + scaleIn(initialScale = .95F) + slideInVertically(initialOffsetY = { it / 4 }),
                exit = fadeOut() + scaleOut(targetScale = .95F) + slideOutVertically(targetOffsetY = { it / 4 })
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 26.dp),
                ) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth()
                            .weight(.5F)
                    )
                    LegacyFlexcode(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        viewModel = viewModel,
                        sheetState = sheetState,
                        toBack = { commerceSessionId ->
                            commerceSessionId?.let { id ->
                                CommerceSessionWorker.execute(context, id)
                            }
                            viewModel.openLegacyCard.value = false
                            viewModel.amount.value = null
                            viewModel.brand.value = null
                        },
                        toDetails = { openBottomSheet(SheetScreen.PaymentDetails(it)) }
                    )
                    Spacer(modifier = Modifier.height(26.dp))
                    Spacer(
                        modifier = Modifier
                            .height(24.dp)
                            .navigationBarsPadding()
                    )
                }
            }
            AnimatedVisibility( // Confirm Payment Card Dim
                modifier = Modifier.zIndex(.4F),
                visible = showNextGenFlexcode && sheetState.isVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val interactionSource = remember { MutableInteractionSource() }
                Canvas(modifier = Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = interactionSource, indication = null)
                    {
                        if (viewModel.sheetScreen is SheetScreen.PaymentDetails) {
                            closeBottomSheet()
                        }
                    },
                    onDraw = { drawRect(color = Color.Black.copy(alpha = .4F)) })
            }
            AnimatedVisibility( // Confirm Payment Card
                modifier = Modifier.zIndex(.3F),
                visible = showNextGenFlexcode,
                enter = fadeIn(animationSpec = tween(500)) + scaleIn(
                    initialScale = .95F,
                    animationSpec = tween(500)
                ),
                exit = fadeOut(animationSpec = tween(500)) + scaleOut(
                    targetScale = .8F,
                    animationSpec = tween(500)
                )
            ) {
                val key by remember { mutableStateOf(commerceSession?.data?.id) }
                val vm = viewModel(
                    key = key,
                    initializer = {
                        ConfirmViewModel(
                            session = viewModel.commerceSession,
                            interactor = Spend.interactor,
                        )
                    }, viewModelStoreOwner = object : ViewModelStoreOwner {
                        override val viewModelStore: ViewModelStore
                            get() = viewModel.confirmViewModelStore
                    }
                )

                DisposableEffect(showNextGenFlexcode) {
                    onDispose {
                        if (!showNextGenFlexcode) {
                            viewModel.confirmViewModelStore.clear()
                            if (sheetState.isVisible && viewModel.sheetScreen is SheetScreen.PaymentDetails) {
                                closeBottomSheet()
                            }
                        }
                    }
                }

                val interactionSource = remember { MutableInteractionSource() }
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            viewModel.deleteCommerceSessionData()
                            commerceSession?.data?.id?.let { id ->
                                viewModel.closeCommerceSession(context, id)
                            }
                        },
                ) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth()
                            .weight(.5F)
                    )
                    ConfirmCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .systemBarsPadding()
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { }
                            )
                            .padding(horizontal = 26.dp),
                        viewModel = vm,
                        sheetState = sheetState,
                        onClose = {
                            viewModel.deleteCommerceSessionData()/*close = true*/
                            commerceSession?.data?.id?.let { id ->
                                viewModel.closeCommerceSession(context, id)
                            }
                        },
                        onTransaction = { toBack() },
                        toDetails = {
                            openBottomSheet(SheetScreen.PaymentDetails(it))
                        },
                        toAssets = { openBottomSheet(SheetScreen.Assets) }
                    )
                    Spacer(modifier = Modifier.height(26.dp))
                    Spacer(
                        modifier = Modifier
                            .height(24.dp)
                            .navigationBarsPadding()
                    )
                }
            }
            /**
             * Amount Screen
             */
            AnimatedVisibility(
                modifier = Modifier.zIndex(.3F),
                visible = showAmount,
                enter = fadeIn(animationSpec = tween(500)) + scaleIn(
                    initialScale = .95F,
                    animationSpec = tween(500)
                ),
                exit = fadeOut(animationSpec = tween(500)) + scaleOut(
                    targetScale = .8F,
                    animationSpec = tween(500)
                )
            ) {
                val brand by viewModel.brand.collectAsStateWithLifecycle()
                brand?.let { b ->
                    val vm = viewModel<InputAmountViewModel>()
                    InputAmountScreen(
                        modifier = Modifier.fillMaxSize(),
                        brand = b,
                        sheetState = sheetState,
                        viewModel = vm,
                        spendViewModel = viewModel,
                        toBack = {
                            viewModel.openAmount.value = false
                            vm.formatter.clear()
                            commerceSession?.data?.id?.let { id ->
                                viewModel.closeCommerceSession(context, id)
                            }
                        },
                        toAssets = {
                            assetsViewModel.assetsScreen.value = AssetsScreen.Assets
                            openBottomSheet(SheetScreen.Assets)
                        },
                        toAmountDetails = { asset, amount ->
                            openBottomSheet(SheetScreen.AmountDetails(asset, amount))
                        },
                    )
                }
            }
            SettingsPopup(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(color = MaterialTheme.colorScheme.tertiaryContainer)
                    .offset(y = (0).dp),
                viewModel = settingsPopupViewModel,
                toPlaces = { openBottomSheet(SheetScreen.PlacesToPay) },
                toFlexaId = { toManageAccount() },
                toHowTo = { },
                toReport = {
                    if (context is Activity) settingsPopupViewModel.reportAnIssue(context)
                }
            )
            val snackbarHostState = remember { SnackbarHostState() }
            Scaffold(
                modifier = modifier
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    AnimatedVisibility(
                        visible = !isAssetsState,
                        enter = slideInVertically(
                            animationSpec = tween(
                                durationMillis = 700,
                                delayMillis = 600
                            )
                        )
                                + fadeIn(
                            animationSpec = tween(
                                durationMillis = 700,
                                delayMillis = 600
                            )
                        ),
                    ) {
                        TopAppBar(
                            title = { Text(text = stringResource(id = R.string.pay_with_flexa)) },
                            navigationIcon = {
                                IconButton(
                                    onClick = { toBack() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null
                                    )
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { settingsPopupViewModel.switch() }) {
                                    Icon(
                                        modifier = Modifier.size(30.dp),
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = null
                                    )
                                }
                            },
                            scrollBehavior = scrollBehavior
                        )
                    }
                    val blur by animateDpAsState(
                        targetValue = if (showNextGenFlexcode || showLegacyFlexcode)
                            4.dp else 0.dp,
                        animationSpec = tween(
                            durationMillis = 500,
                            delayMillis = 500
                        ), label = "blur"
                    )
                    val density = LocalDensity.current
                    AnimatedVisibility(
                        visible = isAssetsState,
                        exit = shrinkVertically(
                            targetHeight = { with(density) { 56.dp.toPx() }.toInt() },
                            animationSpec = tween(durationMillis = 1000, delayMillis = 300)
                        ) + fadeOut(animationSpec = tween(durationMillis = 500)),
                    ) {
                        LargeTopAppBar(
                            modifier = Modifier.blur(
                                blur,
                                edgeTreatment = BlurredEdgeTreatment.Unbounded
                            ),
                            colors = TopAppBarDefaults.largeTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            title = { Text(text = stringResource(id = R.string.pay_with_flexa)) },
                            navigationIcon = {
                                IconButton(
                                    onClick = { toBack() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null
                                    )
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { settingsPopupViewModel.switch() }) {
                                    Icon(
                                        modifier = Modifier.size(30.dp),
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = null
                                    )
                                }
                            },
                            scrollBehavior = scrollBehavior
                        )
                    }
                }
            ) { padding ->
                val blur by animateDpAsState(
                    targetValue = if (showNextGenFlexcode || showLegacyFlexcode)
                        4.dp else 0.dp,
                    animationSpec = tween(durationMillis = 500, delayMillis = 500),
                    label = "blur"
                )
                Spend(
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(blur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                        .padding(
                            top = padding.calculateTopPadding(),
                            bottom = padding.calculateBottomPadding()
                        ),
                    viewModel = viewModel,
                    assetsViewModel = assetsViewModel,
                    brandsViewModel = brandsViewModel,
                    sheetState = sheetState,
                    toAssets = {
                        assetsViewModel.assetsScreen.value = AssetsScreen.Assets
                        openBottomSheet(SheetScreen.Assets)
                    },
                    toAddAssets = { toBack() },
                    toAssetInfo = { asset ->
                        openBottomSheet(SheetScreen.AssetDetails(asset))
                    },
                    toEdit = { toEdit.invoke() },
                    toUrl = { url -> toUrl(url) }
                )
            }
        }
    }

    ErrorDialog(errorHandler = viewModel.errorHandler) {

    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
fun SpendScreenPreview() {
    FlexaTheme {
        SpendScreen(
            modifier = Modifier.fillMaxSize(),
            viewModel = SpendViewModel(FakeInteractor()),
            assetsViewModel = AssetsViewModel(
                FakeInteractor(), MutableStateFlow(MockFactory.getMockSelectedAsset())
            ),
            brandsViewModel = BrandsViewModel(FakeInteractor()),
            settingsPopupViewModel = PopupViewModel(),
            toBack = {},
            toEdit = {},
            toManageAccount = {},
            toUrl = {}
        )
    }
}