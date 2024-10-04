package com.flexa.spend.main.confirm

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.flexa.core.entity.CommerceSession
import com.flexa.core.shared.ErrorDialog
import com.flexa.core.theme.FlexaTheme
import com.flexa.core.view.FlexaLogo
import com.flexa.spend.MockFactory
import com.flexa.spend.R
import com.flexa.spend.Spend
import com.flexa.spend.Transaction
import com.flexa.spend.domain.FakeInteractor
import com.flexa.spend.getAmount
import com.flexa.spend.getAmountLabel
import com.flexa.spend.getSpendableBalance
import com.flexa.spend.hasBalanceRestrictions
import com.flexa.spend.logo
import com.flexa.spend.main.assets.AssetsViewModel
import com.flexa.spend.main.ui_utils.BalanceRestrictionsDialog
import com.flexa.spend.main.ui_utils.SpendAsyncImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Composable
fun ConfirmCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    elevation: Dp = 0.dp,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    viewModel: ConfirmViewModel,
    assetsViewModel: AssetsViewModel,
    onClose: () -> Unit,
    onTransaction: (Transaction) -> Unit = {},
    toDetails: (StateFlow<CommerceSession?>) -> Unit,
    toAssets: () -> Unit = {},
) {
    val palette = MaterialTheme.colorScheme
    val session by viewModel.session.collectAsStateWithLifecycle()
    val previewMode = LocalInspectionMode.current

    BackHandler { onClose() }

    LaunchedEffect(viewModel) {
        launch {
            viewModel.transaction.collect { transaction ->
                transaction?.let {
                    try {
                        onTransaction.invoke(it)
                    } finally {
                        Spend.onTransactionRequest?.invoke(
                            Result.success(it)
                        )
                    }
                }
            }
        }
    }

    Card(
        modifier = modifier.animateContentSize(),
        shape = shape,
        elevation = CardDefaults.cardElevation(elevation),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {

        Row(// Toolbar
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 8.dp, top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FlexaLogo(
                    modifier = Modifier.size(22.dp),
                    shape = RoundedCornerShape(3.dp),
                    colors = listOf(Color.White, palette.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "flexa",
                    style = MaterialTheme.typography.titleLarge
                        .copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.onSurface
                        ),
                )
            }
            Row {
                IconButton(onClick = {
                    toDetails(viewModel.session)
                }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = null,
                        tint = palette.onSurface
                    )
                }
                IconButton(onClick = { onClose() }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = palette.onSurface
                    )
                }
            }
        }

        val density = LocalDensity.current
        var maxWidth by remember { mutableStateOf(1000.dp) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, bottom = 18.dp, top = 0.dp)
                .onGloballyPositioned {
                    maxWidth = density.run { it.size.width.toDp() }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(36.dp))
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                if (!previewMode) {
                    SpendAsyncImage(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        imageUrl = session?.data?.brand?.logoUrl,
                        crossfadeDuration = 1000
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color = palette.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "F",
                            style = MaterialTheme.typography.headlineLarge.copy(palette.onPrimary)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.pay) +
                        " ${session?.data?.brand?.name ?: ""}",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.W600,
                    color = palette.onSurface
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box {
                val text by remember {
                    derivedStateOf { session.getAmountLabel() }
                }
                AnimatedContent(
                    targetState = session.getAmount(),
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInVertically { width -> width } +
                                    fadeIn() togetherWith slideOutVertically()
                            { width -> -width } + fadeOut()
                        } else {
                            slideInVertically { width -> -width } +
                                    fadeIn() togetherWith slideOutVertically()
                            { width -> width } + fadeOut()
                        }.using(SizeTransform(clip = false))
                    }, label = "price_animation"
                ) { state ->
                    state
                    Text(
                        text = text,
                        style = TextStyle(
                            fontSize = 45.sp,
                            fontWeight = FontWeight.W500,
                            color = palette.onSurface
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            androidx.compose.animation.AnimatedVisibility(
                modifier = Modifier,
                visible = !viewModel.completed,
                exit = shrinkVertically() + fadeOut(),
            ) {
                val selectedAsset by if (!previewMode) Spend.selectedAsset.collectAsStateWithLifecycle()
                else remember { mutableStateOf(MockFactory.getMockSelectedAsset()) }
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(
                                topStart = 24.dp,
                                topEnd = 24.dp,
                                bottomStart = 4.dp,
                                bottomEnd = 4.dp
                            )
                        )
                        .height(70.dp),
                    colors = ListItemDefaults.colors(containerColor = palette.background),
                    leadingContent = {
                        if (!previewMode) {
                            AsyncImage(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape),
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(selectedAsset?.asset?.logo())
                                    .decoderFactory(SvgDecoder.Factory())
                                    .build(),
                                contentDescription = null,
                            )
                        } else {
                            Canvas(
                                modifier = Modifier.size(38.dp),
                                onDraw = { drawCircle(palette.tertiary) })
                        }
                    },
                    headlineContent = {
                        Text(
                            text = "Using", style = TextStyle(
                                fontSize = 18.sp, fontWeight = FontWeight.Normal
                            )
                        )
                    },
                    supportingContent = {
                        Text(
                            text = selectedAsset?.asset?.assetData?.displayName ?: "",
                            style = TextStyle(
                                fontSize = 13.sp, fontWeight = FontWeight.Normal
                            )
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = { toAssets() }) {
                            Icon(
                                modifier = Modifier.size(22.dp),
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                tint = palette.outline
                            )
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
            androidx.compose.animation.AnimatedVisibility(viewModel.payProgress) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
            val completeButtonHeight by remember { mutableIntStateOf(34) }
            val transition = updateTransition(viewModel.completed, label = "complete button state")
            val topRadius by transition.animateDp(label = "topRadius",
                transitionSpec = { tween(500) }
            ) { state ->
                if (!state) 4.dp else (completeButtonHeight / 2).dp
            }
            val bottomRadius by transition.animateDp(label = "bottomRadius",
                transitionSpec = { tween(500) }) { state ->
                if (!state) 16.dp else (completeButtonHeight / 2).dp
            }
            val height by transition.animateDp(label = "height",
                transitionSpec = { tween(500) }) { state ->
                if (!state) 50.dp else completeButtonHeight.dp
            }
            val width by transition.animateDp(label = "height",
                transitionSpec = { tween(500) }) { state ->
                if (!state) maxWidth else 100.dp
            }
            val buttonColor1 by transition.animateColor(label = "buttonColor1") { state ->
                if (!state) Color(0xFF006CFF) else Color(0xFF8ABCFF)
            }
            val buttonColor2 by transition.animateColor(label = "buttonColor1") { state ->
                if (!state) Color(0xFF2A00FF) else Color(0xFFBEB2FF)
            }
            val buttonColor3 by transition.animateColor(label = "buttonColor1") { state ->
                if (!state) Color(0xFF7800FF) else Color(0xFFF8D0FF)
            }
            val textColor1 by transition.animateColor(label = "textColor1") { state ->
                if (!state) Color.White else Color(0xFF2A00FF)
            }
            val textColor2 by transition.animateColor(label = "textColor1") { state ->
                if (!state) Color.White else Color(0xFF7800FF)
            }
            val bottomPadding by transition.animateDp(label = "bottomPadding",
                transitionSpec = { tween(500) }) { state ->
                if (!state) 0.dp else (42 - 18).dp
            }

            CompositionLocalProvider(
                LocalMinimumInteractiveComponentSize provides 0.dp,
            ) {
                val showBalanceRestrictions by viewModel.showBalanceRestrictions.collectAsStateWithLifecycle()
                if (showBalanceRestrictions) {
                    BalanceRestrictionsDialog(
                        modifier = Modifier.padding(8.dp),
                        viewModel = assetsViewModel
                    ) { viewModel.showBalanceRestrictions(false) }
                }
                val selectedAsset by assetsViewModel.selectedAssetWithBundles.collectAsStateWithLifecycle()
                val hasBalanceRestrictions by remember {
                    derivedStateOf { selectedAsset?.asset?.hasBalanceRestrictions() ?: false }
                }
                val totalBalanceEnough by remember {
                    derivedStateOf {
                        val assetAmount =
                            selectedAsset?.asset?.balanceBundle?.total ?: BigDecimal.ZERO
                        val amount = session?.getAmount() ?: BigDecimal.ZERO
                        assetAmount >= amount
                    }
                }
                val availableBalanceEnough by remember {
                    derivedStateOf {
                        val assetAmount =
                            selectedAsset?.asset?.getSpendableBalance() ?: BigDecimal.ZERO
                        val amount = session?.getAmount() ?: BigDecimal.ZERO
                        assetAmount >= amount
                    }
                }
                val enough by remember(session) {
                    derivedStateOf {
                        val res = totalBalanceEnough && availableBalanceEnough
                        if (res) viewModel.showBalanceRestrictions(false)
                        res
                    }
                }
                val canProceed by remember(session) {
                    derivedStateOf {
                        !viewModel.buttonsBlocked && enough
                    }
                }
                val wrongAvailableState by remember {
                    derivedStateOf { !availableBalanceEnough && hasBalanceRestrictions && totalBalanceEnough }
                }
                TextButton(
                    modifier = Modifier
                        .size(width, height)
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(
                                topStart = topRadius,
                                topEnd = topRadius,
                                bottomStart = bottomRadius,
                                bottomEnd = bottomRadius
                            )
                        )
                        .background(
                            brush = if (enough) Brush.linearGradient(
                                listOf(buttonColor1, buttonColor2, buttonColor3)
                            ) else SolidColor(MaterialTheme.colorScheme.outline)
                        ),
                    shape = RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 4.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    ),
                    contentPadding = PaddingValues(0.dp),
                    onClick = {
                        when {
                            canProceed -> viewModel.payNow()
                            wrongAvailableState -> viewModel.showBalanceRestrictions(
                                true
                            )
                        }
                    }) {
                    val context = LocalContext.current
                    val payButtonText by remember {
                        derivedStateOf {
                            when {
                                viewModel.completed -> context.resources.getString(R.string.done)
                                viewModel.payProgress -> context.resources.getString(R.string.processing) + "..."
                                viewModel.patchProgress -> context.resources.getString(R.string.updating) + "..."
                                !totalBalanceEnough -> "Not enough ${selectedAsset?.asset?.assetData?.displayName}"
                                !availableBalanceEnough -> context.getString(R.string.balance_not_yet_available)
                                else -> context.resources.getString(R.string.pay_now)
                            }
                        }
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        modifier = Modifier,
                        visible = viewModel.completed,
                        enter = scaleIn(initialScale = 1.2F) + fadeIn(),
                    ) {
                        ConfirmDone(modifier = Modifier)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    AnimatedContent(
                        targetState = payButtonText,
                        transitionSpec = {
                            if (targetState.length > initialState.length) {
                                slideInHorizontally { width -> width } +
                                        fadeIn() togetherWith slideOutHorizontally()
                                { width -> -width } + fadeOut()
                            } else {
                                slideInHorizontally { width -> -width } +
                                        fadeIn() togetherWith slideOutHorizontally()
                                { width -> width } + fadeOut()
                            }.using(SizeTransform(clip = false))
                        }, label = "Text"
                    ) { state ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = state,
                                style = TextStyle(
                                    brush = Brush.linearGradient(listOf(textColor1, textColor2)),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.W500,
                                )
                            )
                            androidx.compose.animation.AnimatedVisibility(
                                modifier = Modifier.padding(4.dp),
                                visible = wrongAvailableState
                            ) {
                                Icon(
                                    modifier = Modifier.size(20.dp),
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(bottomPadding))
        }
    }
    ErrorDialog(errorHandler = viewModel.errorHandler)
}

@Composable
fun ConfirmDone(
    modifier: Modifier = Modifier,
    tint: Color = Color.Blue
) {
    var launched by remember { mutableStateOf(false) }
    val angle by animateFloatAsState(
        targetValue = if (launched) 0F else 180F,
        animationSpec = tween(durationMillis = 700, easing = EaseOutBack), label = ""
    )
    LaunchedEffect(Unit) {
        launched = true
    }
    Icon(
        modifier = modifier.rotate(angle),
        imageVector = Icons.Rounded.Check,
        contentDescription = null,
        tint = tint
    )
}

@Preview(name = "Light")
@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun ConfirmDialogPreview() {
    FlexaTheme {
        ConfirmCard(
            modifier = Modifier
                .width(320.dp),
            elevation = 8.dp,
            viewModel = viewModel(initializer = {
                ConfirmViewModel(
                    session = MutableStateFlow(MockFactory.getCommerceSession()),
                    interactor = FakeInteractor()
                )
            }),
            assetsViewModel = AssetsViewModel(
                FakeInteractor(), MutableStateFlow(MockFactory.getMockSelectedAsset())
            ),
            onClose = {},
            toDetails = {},
        )
    }
}
