package com.flexa.spend.main.keypad

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flexa.core.shared.Promotion
import com.flexa.core.theme.FlexaTheme
import com.flexa.core.toCurrencySign
import com.flexa.core.view.AutoSizeText
import com.flexa.spend.MockFactory
import com.flexa.spend.R
import com.flexa.spend.Spend
import com.flexa.spend.containsAuthorization
import com.flexa.spend.domain.FakeInteractor
import com.flexa.spend.getAmount
import com.flexa.spend.getDiscount
import com.flexa.spend.getPromotion
import com.flexa.spend.getSpendableBalance
import com.flexa.spend.hasBalanceRestrictions
import com.flexa.spend.isDark
import com.flexa.spend.lightenColor
import com.flexa.spend.main.assets.AssetsBottomSheet
import com.flexa.spend.main.assets.AssetsViewModel
import com.flexa.spend.main.main_screen.SpendDragHandler
import com.flexa.spend.main.main_screen.SpendLifecycleRelatedMethods
import com.flexa.spend.main.main_screen.SpendViewModel
import com.flexa.spend.main.ui_utils.BalanceRestrictionsDialog
import com.flexa.spend.main.ui_utils.KeepScreenOn
import com.flexa.spend.main.ui_utils.SpendAsyncImage
import com.flexa.spend.main.ui_utils.rememberSelectedAsset
import com.flexa.spend.positive
import com.flexa.spend.shiftHue
import com.flexa.spend.toColor
import kotlinx.coroutines.flow.MutableStateFlow
import java.math.BigDecimal
import kotlin.math.roundToInt


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InputAmountScreen(
    modifier: Modifier = Modifier,
    viewModel: InputAmountViewModel,
    spendViewModel: SpendViewModel,
    assetsViewModel: AssetsViewModel,
    toUrl: ((@ParameterName("url") String) -> Unit),
    toBack: (() -> Unit),
) {
    val context = LocalContext.current
    val palette = MaterialTheme.colorScheme

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var sheetType by remember { mutableStateOf(SheetType.ASSETS) }

    val selectedAsset by rememberSelectedAsset()
    val brand by spendViewModel.selectedBrand.collectAsStateWithLifecycle()
    val unitOfAccount by spendViewModel.unitOfAccount.collectAsStateWithLifecycle()
    val amount by viewModel.formatter.dataAsFlow.collectAsStateWithLifecycle()
    val progress by spendViewModel.progress.collectAsStateWithLifecycle()
    val timeout by spendViewModel.timeout.collectAsStateWithLifecycle()
    val inputState by viewModel.inputState.collectAsStateWithLifecycle(
        initialValue = InputState.Unspecified
    )
    val inputStateDelayed by viewModel.inputStateDelayed.collectAsStateWithLifecycle(
        initialValue = InputState.Unspecified
    )
    val commerceSession by spendViewModel.commerceSession.collectAsStateWithLifecycle()

    val discount by remember {
        derivedStateOf {
            brand?.promotions?.getPromotion(selectedAsset?.asset?.livemode)
                ?.getDiscount(amount ?: "0") ?: BigDecimal.ZERO
        }
    }
    val accentColor by remember {
        mutableStateOf(
            brand?.color?.toColor() ?: palette.primary
        )
    }

    val returnBack = {
        spendViewModel.setBrand(null)
        spendViewModel.stopProgress()
        spendViewModel.cancelTimeout()
        if (commerceSession?.containsAuthorization() == false) {
            commerceSession?.data?.id?.let { id ->
                spendViewModel.closeCommerceSession(context, id)
            }
        }
        toBack()
    }

    SpendLifecycleRelatedMethods(assetsViewModel)

    BackHandler {
        returnBack()
    }

    LaunchedEffect(commerceSession) {
        val legacy = commerceSession?.data?.isLegacy == true
        val containsAuthorization = commerceSession?.containsAuthorization() ?: false
        if (legacy && containsAuthorization) {
            returnBack()
        }
    }

    val density = LocalDensity.current
    val shake = remember { Animatable(0f) }
    var shakeTrigger by remember { mutableLongStateOf(0L) }
    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger != 0L) {
            for (i in 0..5) {
                when (i % 2) {
                    0 -> shake.animateTo(
                        with(density) { 20.dp.toPx() }, spring(stiffness = 13_000f)
                    )

                    else -> shake.animateTo(
                        with(density) { -20.dp.toPx() }, spring(stiffness = 13_000f)
                    )
                }
            }
            shake.animateTo(0f)
        }
    }

    LaunchedEffect(brand) {
        viewModel.setMinMaxValue(brand)
    }

    LaunchedEffect(inputState) {
        when {
            inputState == InputState.Unspecified -> {}
            inputState != InputState.Fine -> {
                if (inputState is InputState.Max) {
                    shakeTrigger = System.currentTimeMillis()
                }
            }
        }
    }

    KeepScreenOn()

    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .systemBarsPadding()
            .clickable(interactionSource = interactionSource, indication = null) {}
            .background(palette.surface),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val configuration = LocalConfiguration.current
        val smallScreen by remember {
            mutableStateOf(configuration.screenWidthDp < 370)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { returnBack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = amount?.isNotBlank() == true
            ) {
                IconButton(
                    content = {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            tint = palette.onBackground,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        sheetType = SheetType.AMOUNT
                        showBottomSheet = true
                    }
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SpendAsyncImage(
                modifier = Modifier
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .size(60.dp),
                imageUrl = brand?.logoUrl,
                placeholder = BrushPainter(
                    SolidColor(Color.White.copy(.3F))
                ),
            )
            val discountPositive by remember {
                derivedStateOf {
                    brand?.getPromotion(selectedAsset?.asset?.livemode)?.positive(amount ?: "")
                        ?: false
                }
            }
            val promotion by remember {
                derivedStateOf {
                    brand?.promotions?.getPromotion(selectedAsset?.asset?.livemode)
                }
            }
            val hasPromotion by remember { derivedStateOf { promotion != null } }
            AnimatedVisibility(
                visible = hasPromotion,
                enter = scaleIn(initialScale = 1.2F) + fadeIn(),
                exit = scaleOut(targetScale = 1.2F) + fadeOut()
            ) {
                val dark = isSystemInDarkTheme()
                val background by animateColorAsState(
                    if (!discountPositive) palette.outline.copy(alpha = .3F)
                    else brand?.color?.toColor()?.copy(alpha = .2F)
                        ?: palette.primary.copy(alpha = .2F),
                    label = "promotion background"
                )
                val contentColor by animateColorAsState(
                    if (discountPositive) {
                        if (dark) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            brand?.color?.toColor() ?: Color.Magenta
                        }
                    } else Color.Gray, label = "promotion text color",
                    animationSpec = tween(500)
                )
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        modifier = Modifier.height(30.dp),
                        enabled = promotion?.url != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = background,
                            disabledContainerColor = background
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        onClick = { promotion?.url?.let(toUrl) }
                    ) {
                        AnimatedVisibility(discountPositive) {
                            Row {
                                Icon(
                                    modifier = Modifier.size(20.dp),
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = contentColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                        }
                        val promotionText by remember {
                            derivedStateOf {
                                if (!discountPositive) {
                                    promotion?.label ?: ""
                                } else {
                                    "${context.getString(R.string.saving)} " +
                                            unitOfAccount.toCurrencySign() +
                                            (amount?.getAmount() ?: BigDecimal.ZERO).coerceAtMost(
                                                discount
                                            ).setScale(2).toPlainString()
                                }
                            }
                        }
                        Text(
                            modifier = Modifier.animateContentSize(),
                            text = promotionText,
                            color = contentColor,
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        AnimatedVisibility(!discountPositive) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = contentColor
                            )
                        }
                    }
                }
            }
        }
        AmountText(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(x = shake.value.roundToInt(), y = 0) }
                .padding(horizontal = 32.dp),
            formatter = viewModel.formatter,
            accentColor = accentColor,
        )
        if (!smallScreen) Spacer(modifier = Modifier.height(16.dp))
        val buttonVisible by remember {
            derivedStateOf {
                inputStateDelayed is InputState.Fine ||
                        inputStateDelayed is InputState.Unspecified
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val btnColor by animateColorAsState(
                if (buttonVisible) palette.secondaryContainer else palette.surface,
                animationSpec = tween(700),
                label = "button color"
            )
            FilledTonalButton(
                modifier = Modifier.animateContentSize(),
                contentPadding = PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = btnColor,
                    disabledContainerColor = btnColor,
                    disabledContentColor = palette.onSecondaryContainer
                ),
                enabled = buttonVisible,
                onClick = {
                    sheetType = SheetType.ASSETS
                    showBottomSheet = true
                }
            ) {
                val text by remember {
                    derivedStateOf {
                        when (inputStateDelayed) {
                            is InputState.Min ->
                                "${context.getString(R.string.minimum_amount)}: \$${brand?.legacyFlexcodes?.firstOrNull()?.amount?.minimum ?: ""}"

                            is InputState.Max ->
                                "${context.getString(R.string.maximum_amount)}: \$${brand?.legacyFlexcodes?.firstOrNull()?.amount?.maximum ?: ""}"

                            else -> "${context.getString(R.string.using)} ${selectedAsset?.asset?.assetData?.displayName}"
                        }
                    }
                }
                if (buttonVisible)
                    Spacer(modifier = Modifier.width(4.dp))
                AnimatedContent(
                    targetState = text, label = "button text",
                    transitionSpec = {
                        expandHorizontally() + fadeIn() togetherWith
                                shrinkHorizontally()
                    }
                ) { t ->
                    Text(
                        modifier = Modifier.animateContentSize(),
                        text = t,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                if (buttonVisible)
                    Spacer(modifier = Modifier.width(4.dp))
                androidx.compose.animation.AnimatedVisibility(
                    visible = buttonVisible,
                    enter = slideInVertically(
                        animationSpec = tween(300, delayMillis = 300, easing = EaseOutBack)
                    ) + fadeIn(animationSpec = tween(300, delayMillis = 300)),
                    exit = slideOutVertically(tween(100))
                ) {
                    val rotate by animateFloatAsState(
                        targetValue = if (showBottomSheet && sheetType == SheetType.ASSETS) 180f else 0f,
                        animationSpec = tween(500),
                        label = "assets button angle"
                    )
                    Icon(
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationX = rotate },
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            }

            if (!smallScreen) Spacer(modifier = Modifier.height(16.dp))
            Keypad(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                aspectRatio = if (smallScreen) 1.8f else 1.5f
            ) { button ->
                val data = viewModel.formatter.getInputData(button)
                when (val state = viewModel.getInputState(data)) {
                    is InputState.Max -> {
                        viewModel.setInputState(state)
                    }

                    else -> {
                        viewModel.formatter.append(button)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            PayButton(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .systemBarsPadding()
                    .height(54.dp),
                viewModel = viewModel,
                assetsViewModel = assetsViewModel,
                spendViewModel = spendViewModel,
                inputState = inputState,
                colors = listOf(
                    brand?.color.toColor().shiftHue(10f),
                    brand?.color.toColor(),
                    brand?.color.toColor().shiftHue(-10f)
                ),
                onClick = {
                    val enabled =
                        (inputState is InputState.Fine || inputState is InputState.Max) && !progress
                    if (enabled) {
                        selectedAsset?.let { asset ->
                            spendViewModel.createCommerceSession(
                                brandId = brand?.id ?: "",
                                amount = amount ?: "",
                                paymentAssetId = asset.asset.assetId
                            )
                        }
                    } else {
                        shakeTrigger = System.currentTimeMillis()
                    }
                }
            )
            Spacer(
                modifier = Modifier
                    .height(20.dp)
                    .navigationBarsPadding()
            )
        }
    }
    AnimatedVisibility(progress, enter = fadeIn(), exit = fadeOut()) {
        val inso = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 70.dp)
                .statusBarsPadding()
                .clickable(interactionSource = inso, indication = null) {}
        ) { }
    }
    if (timeout) {
        AlertDialog(
            onDismissRequest = { },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            shape = RoundedCornerShape(25.dp),
            icon = {
                Box {
                    Icon(
                        imageVector = Icons.Rounded.CreditCard,
                        contentDescription = null
                    )
                    Icon(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(AlertDialogDefaults.containerColor)
                            .align(Alignment.BottomEnd),
                        imageVector = Icons.Rounded.WatchLater,
                        contentDescription = null
                    )
                }
            },
            title = {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Too long execution",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                    )
                )
            },
            text = {
                Text(
                    text = "Payment executes too long, do you want to make another attempt?",
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp
                    )
                )
            },
            dismissButton = {
                TextButton(onClick = { spendViewModel.initCloseSessionTimeout() }) {
                    Text(text = "Wait")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    returnBack.invoke()
                    spendViewModel.cancelTimeout()
                }) {
                    Text(text = "Cancel Payment")
                }
            }
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            dragHandle = { SpendDragHandler() },
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
        ) {
            when (sheetType) {
                SheetType.ASSETS -> {
                    AssetsBottomSheet(
                        viewModel = assetsViewModel,
                        toUrl = { toUrl(it) },
                        toBack = { showBottomSheet = false }
                    )
                }

                SheetType.AMOUNT -> {
                    selectedAsset?.let { asset ->
                        AmountDetailsScreen(
                            modifier = Modifier.fillMaxWidth(),
                            viewModel = viewModel(
                                initializer = {
                                    AmountDetailViewModel(Spend.interactor)
                                }),
                            assetsViewModel = assetsViewModel,
                            assetBundle = asset,
                            amount = amount ?: "",
                            promotions = brand?.promotions,
                            toLearnMore = { toUrl(context.getString(R.string.learn_more_link)) },
                        )
                    }
                }
            }
        }
    }
}

private enum class SheetType {
    ASSETS, AMOUNT
}

@Composable
internal fun PayButton(
    modifier: Modifier = Modifier,
    viewModel: InputAmountViewModel,
    assetsViewModel: AssetsViewModel,
    spendViewModel: SpendViewModel,
    inputState: InputState,
    colors: List<Color> = listOf(Color.Red, Color.Yellow),
    onClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val progress by spendViewModel.progress.collectAsStateWithLifecycle()
    val selectedAsset by assetsViewModel.selectedAssetBundle.collectAsStateWithLifecycle()
    val amount by viewModel.formatter.dataAsFlow.collectAsStateWithLifecycle()
    val totalBalanceEnough by remember {
        derivedStateOf {
            val assetAmount =
                selectedAsset?.asset?.balanceBundle?.total ?: BigDecimal.ZERO
            val paymentAmount = amount.getAmount()
            assetAmount >= paymentAmount
        }
    }
    val availableBalanceEnough by remember {
        derivedStateOf {
            val assetAmount =
                selectedAsset?.asset?.getSpendableBalance() ?: BigDecimal.ZERO
            val paymentAmount = amount.getAmount()
            assetAmount >= paymentAmount
        }
    }
    val enough by remember {
        derivedStateOf {
            val res = totalBalanceEnough && availableBalanceEnough
            if (res) viewModel.showBalanceRestrictions(false)
            res
        }
    }
    val hasBalanceRestrictions by remember {
        derivedStateOf { selectedAsset?.asset?.hasBalanceRestrictions() ?: false }
    }
    val enabled by remember(inputState, progress, enough) {
        derivedStateOf {
            enough && (inputState is InputState.Fine || inputState is InputState.Max) && !progress
        }
    }
    val wrongAvailableState by remember {
        derivedStateOf { !availableBalanceEnough && hasBalanceRestrictions && totalBalanceEnough }
    }
    val text by remember {
        derivedStateOf {
            when {
                progress -> "${context.getString(R.string.processing)}..."
                !totalBalanceEnough -> "Not enough ${selectedAsset?.asset?.assetData?.displayName}"
                !availableBalanceEnough -> "${context.getString(R.string.balance_not_yet_available)}..."
                else -> context.getString(R.string.confirm)
            }
        }
    }
    val showBalanceRestrictions by viewModel.showBalanceRestrictions.collectAsStateWithLifecycle()
    val shape = MaterialTheme.shapes.large
    TextButton(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                brush = if (enabled) Brush.linearGradient(colors = colors)
                else SolidColor(MaterialTheme.colorScheme.outline)
            ),
        contentPadding = PaddingValues(0.dp),
        shape = shape,
        onClick = {
            when {
                enough -> onClick()
                wrongAvailableState ->
                    viewModel.showBalanceRestrictions(true)
            }
        }
    ) {
        AnimatedContent(
            targetState = text,
            transitionSpec = {
                (expandHorizontally { it / 2 } + fadeIn() + scaleIn(initialScale = .7F))
                    .togetherWith(
                        shrinkHorizontally { it / 2 } + fadeOut() + scaleOut(targetScale = .7F))
            }, label = "Pay Now"
        ) { state ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.animation.AnimatedVisibility(
                    modifier = Modifier.padding(4.dp),
                    visible = progress
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                }
                Text(
                    modifier = Modifier,
                    text = state,
                    color = Color.White,
                    fontWeight = FontWeight.W700,
                    fontSize = 17.sp
                )
                androidx.compose.animation.AnimatedVisibility(
                    modifier = Modifier.padding(4.dp),
                    visible = wrongAvailableState && !progress
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }
    if (showBalanceRestrictions) {
        BalanceRestrictionsDialog(viewModel = assetsViewModel) {
            viewModel.showBalanceRestrictions(false)
        }
    }
}

@Composable
fun AmountText(
    modifier: Modifier = Modifier,
    formatter: Formatter = Formatter(),
    accentColor: Color,
) {
    val dark = isSystemInDarkTheme()
    val text by formatter.dataAsFlow.collectAsStateWithLifecycle(initialValue = formatter.getText())
    val configuration = LocalConfiguration.current
    val smallScreen by remember {
        mutableStateOf(configuration.screenWidthDp < 370)
    }
    val grayColor = Color.LightGray
    val animatedColor by animateColorAsState(
        when {
            text.isNullOrEmpty() -> grayColor
            dark && accentColor.isDark(.2f) -> accentColor.lightenColor(.7f)
            else -> {
                accentColor
            }
        }, label = "animatedColor"
    )
    AutoSizeText(
        modifier = modifier,
        annotatedText = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    fontFeatureSettings = "tnum",
                    brush = when {
                        text.isNullOrEmpty() -> SolidColor(animatedColor)
                        dark && accentColor.isDark(.2f) -> SolidColor(animatedColor)
                        else -> {
                            Brush.linearGradient(
                                listOf(
                                    animatedColor.shiftHue(10F),
                                    animatedColor,
                                    animatedColor.shiftHue(-10F),
                                )
                            )
                        }
                    }
                )
            ) {
                append(formatter.getPrefix())
                if (text != null) append(text)
            }
            withStyle(
                SpanStyle(
                    fontFeatureSettings = "tnum",
                    color = grayColor
                )
            ) { append(formatter.getSuffix()) }
        },
        textStyle = TextStyle(
            fontSize = if (smallScreen) 40.sp else 84.sp,
            fontWeight = FontWeight.W600,
            textAlign = TextAlign.Center,
        )
    )
}

@Preview
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    device = "id:pixel_5"
)
@Preview(device = "id:3.7in WVGA (Nexus One)")
@Composable
private fun KeypadScreenPreview() {
    FlexaTheme {
        InputAmountScreen(
            modifier = Modifier.fillMaxSize(),
            viewModel = InputAmountViewModel().apply {
                formatter.append(Symbol("23.10"))
            },
            spendViewModel = SpendViewModel(FakeInteractor()).apply {
                setBrand(
                    MockFactory.getBrand().run {
                        copy(
                            color = "#8043FF",
                            promotions = listOf(
                                Promotion(
                                    id = "",
                                    amountOff = "10",
                                    percentOff = "50",
                                    livemode = true,
                                    label = "Promotion label",
                                    restrictions = Promotion.Restrictions(
                                        maximumDiscount = "15", minimumAmount = "5",
                                    )
                                )
                            )
                        )
                    }
                )
            },
            assetsViewModel = AssetsViewModel(
                FakeInteractor(), MutableStateFlow(MockFactory.getMockSelectedAsset())
            ),
            toUrl = {},
            toBack = {},
        )
    }
}
