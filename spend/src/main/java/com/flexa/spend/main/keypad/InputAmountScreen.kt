package com.flexa.spend.main.keypad

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flexa.core.shared.Brand
import com.flexa.core.shared.SelectedAsset
import com.flexa.core.theme.FlexaTheme
import com.flexa.core.view.AutoSizeText
import com.flexa.core.view.FlexaProgress
import com.flexa.spend.MockFactory
import com.flexa.spend.R
import com.flexa.spend.domain.FakeInteractor
import com.flexa.spend.main.main_screen.SheetScreen
import com.flexa.spend.main.main_screen.SpendViewModel
import com.flexa.spend.rememberSelectedAsset
import com.flexa.spend.toColor
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun InputAmountScreen(
    modifier: Modifier = Modifier,
    brand: Brand,
    viewModel: InputAmountViewModel,
    spendViewModel: SpendViewModel,
    sheetState: ModalBottomSheetState? = null,
    toBack: (() -> Unit),
    toAmountDetails: ((SelectedAsset, String) -> Unit),
    toAssets: (() -> Unit),
) {

    val scope = rememberCoroutineScope()
    val palette = MaterialTheme.colorScheme
    val selectedAsset by rememberSelectedAsset()
    val amount by viewModel.formatter.dataAsFlow.collectAsStateWithLifecycle()
    val progress by spendViewModel.progress.collectAsStateWithLifecycle()
    val timeout by spendViewModel.timeout.collectAsStateWithLifecycle()
    val inputState by viewModel.amountBoundaries.collectAsStateWithLifecycle(
        initialValue = InputState.Unspecified
    )
    val returnBack = {
        toBack()
        spendViewModel.stopProgress()
    }

    val density = LocalDensity.current
    val shake = remember { Animatable(0f) }
    var shakeTrigger by remember { mutableStateOf(0L) }
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
            inputState != InputState.Fine ->
                shakeTrigger = System.currentTimeMillis()
        }
    }

    BackHandler {
        if (sheetState?.isVisible == true)
            scope.launch { sheetState.hide() }
        else {
            returnBack.invoke()
        }
    }

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
                onClick = { toBack() }) {
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
                        selectedAsset?.let { asset ->
                            amount?.let { toAmountDetails.invoke(asset, it) }
                        }
                    }
                )
            }
        }
        AnimatedContent(
            targetState = progress,
            transitionSpec = {
                (scaleIn(initialScale = .7F) + fadeIn()).togetherWith(scaleOut(targetScale = .7F) + fadeOut())
            }, label = "Icon"
        ) { progressState ->
            if (!progressState) {
                AsyncImage(
                    modifier = Modifier
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .size(60.dp),
                    placeholder = BrushPainter(
                        SolidColor(Color.White.copy(.3F))
                    ),
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(brand.logoUrl)
                        .crossfade(true)
                        .crossfade(500)
                        .build(),
                    contentDescription = null,
                )
            } else {
                FlexaProgress(
                    modifier = Modifier.size(60.dp),
                    roundedCornersSize = 12.dp,
                    borderWidth = 2.dp
                )
            }
        }
        AmountText(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = with(density) { shake.value.toDp() }, y = 0.dp)
                .padding(horizontal = 32.dp),
            formatter = viewModel.formatter,
            colors = listOf(brand.color.toColor(), palette.primary)
        )
        if (!smallScreen) Spacer(modifier = Modifier.height(16.dp))
        val buttonVisible by remember {
            derivedStateOf { inputState is InputState.Fine || inputState is InputState.Unspecified }
        }
        val warningVisible by remember {
            derivedStateOf { inputState is InputState.Min || inputState is InputState.Max }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.animation.AnimatedVisibility(
                visible = buttonVisible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                FilledTonalButton(
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    onClick = toAssets
                ) {
                    val angle by animateFloatAsState(
                        targetValue = if ((sheetState?.isVisible == true)
                            && spendViewModel.sheetScreen is SheetScreen.Assets
                        ) -180F else 0F, label = "assets button angle"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${stringResource(R.string.using)} ${selectedAsset?.asset?.assetData?.displayName}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(angle),
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = warningVisible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val context = LocalContext.current
                val text by remember {
                    derivedStateOf {
                        when(inputState) {
                            is InputState.Min ->
                                "${context.getString(R.string.minimum_amount)}: \$${brand.legacyFlexcodes?.firstOrNull()?.amount?.minimum ?: ""}"
                            is InputState.Max ->
                                "${context.getString(R.string.maximum_amount)}: \$${brand.legacyFlexcodes?.firstOrNull()?.amount?.maximum ?: ""}"
                            else -> ""
                        }
                    }
                }
                Text(
                    modifier = Modifier.height(48.dp),
                    text = text,
                    color = palette.onSurface,
                    textAlign = TextAlign.Center
                )
            }
            if (!smallScreen) Spacer(modifier = Modifier.height(16.dp))
            Keypad(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
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
                inputState = inputState,
                progress = progress,
                colors = listOf(brand.color.toColor(), palette.primary),
                onClick = {
                    selectedAsset?.let { asset ->
                        spendViewModel.createCommerceSession(
                            brandId = brand.id,
                            amount = amount ?: "",
                            assetId = asset.asset.value?.asset ?: "",
                            paymentAssetId = asset.asset.assetId
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(20.dp))
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
}

@Composable
fun PayButton(
    modifier: Modifier = Modifier,
    inputState: InputState,
    progress: Boolean,
    colors: List<Color> = listOf(Color.Red, Color.Yellow),
    onClick: () -> Unit = {},
) {
    val enabled = (inputState is InputState.Fine || inputState is InputState.Max) && !progress
    TextButton(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = if (enabled) Brush.linearGradient(colors = colors)
                else SolidColor(MaterialTheme.colorScheme.outline)
            ),
        contentPadding = PaddingValues(0.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        onClick = { onClick.invoke() }) {
        AnimatedContent(
            targetState = !progress,
            transitionSpec = {
                (expandHorizontally { it / 2 } + fadeIn() + scaleIn(initialScale = .7F))
                    .togetherWith(
                        shrinkHorizontally { it / 2 } + fadeOut() + scaleOut(targetScale = .7F))
            }, label = "Pay Now"
        ) { state ->
            state
            val context = LocalContext.current
            val text by remember {
                derivedStateOf {
                    if (!progress) context.getString(R.string.confirm)
                    else "${context.getString(R.string.processing)}..."
                }
            }
            Text(
                modifier = Modifier,
                text = text,
                color = Color.White,
                fontWeight = FontWeight.W700,
                fontSize = 17.sp
            )
        }
    }
}

@Composable
fun AmountText(
    modifier: Modifier = Modifier,
    formatter: Formatter = Formatter(),
    colors: List<Color> = listOf(Color.Red, Color.Yellow)
) {
    val text by formatter.dataAsFlow.collectAsStateWithLifecycle(initialValue = formatter.getText())
    val configuration = LocalConfiguration.current
    val smallScreen by remember {
        mutableStateOf(configuration.screenWidthDp < 370)
    }
    AutoSizeText(
        modifier = modifier,
        annotatedText = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    fontFeatureSettings = "tnum",
                    brush = if (text.isNullOrEmpty()) SolidColor(
                        MaterialTheme.colorScheme.outline.copy(
                            alpha = .7F
                        )
                    ) else Brush.linearGradient(colors)
                )
            ) {
                append(formatter.getPrefix())
                if (text != null) append(text)
            }
            withStyle(
                SpanStyle(
                    fontFeatureSettings = "tnum",
                    color = MaterialTheme.colorScheme.outline.copy(
                        alpha = .7F
                    )
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

@OptIn(ExperimentalMaterialApi::class)
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
            brand = MockFactory.getMockBrand(),
            viewModel = InputAmountViewModel().apply {
                formatter.append(Symbol("53.13"))
            },
            spendViewModel = SpendViewModel(FakeInteractor()),
            toBack = {},
            toAssets = {},
            toAmountDetails = { _, _ -> },
        )
    }
}
