package com.flexa.spend.main.legacy_flexcode

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flexa.core.entity.CommerceSession
import com.flexa.core.theme.FlexaTheme
import com.flexa.core.view.FlexaLogo
import com.flexa.spend.BuildConfig
import com.flexa.spend.MockFactory
import com.flexa.spend.domain.FakeInteractor
import com.flexa.spend.getAmount
import com.flexa.spend.getAmountLabel
import com.flexa.spend.main.flexcode.PDF417
import com.flexa.spend.main.main_screen.SpendViewModel
import com.flexa.spend.main.ui_utils.MarkdownText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterialApi::class, ExperimentalAnimationApi::class)
@Composable
fun LegacyFlexcode(
    modifier: Modifier = Modifier,
    viewModel: SpendViewModel,
    sheetState: ModalBottomSheetState,
    toBack: (commerceSessionId: String?) -> Unit,
    toDetails: (StateFlow<CommerceSession?>) -> Unit,
) {
    val previewMode = LocalInspectionMode.current
    val brand by viewModel.brand.collectAsStateWithLifecycle()
    val commerceSession by if (!previewMode) viewModel.commerceSession.collectAsStateWithLifecycle()
    else MutableStateFlow(MockFactory.getMockCommerceSessionCompleted()).collectAsState()

    BackHandler(!sheetState.isVisible) {
        toBack.invoke(commerceSession?.data?.id)
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(// Toolbar
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FlexaLogo(
                    modifier = Modifier.size(22.dp),
                    shape = RoundedCornerShape(3.dp),
                    colors = listOf(Color.White, MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "flexa",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
            Row {
                IconButton(onClick = { toDetails(viewModel.commerceSession) }) {
                    Icon(imageVector = Icons.Filled.MoreVert, contentDescription = null)
                }
                IconButton(onClick = { toBack(commerceSession?.data?.id) }) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = null)
                }
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
        if (!previewMode) {
            AsyncImage(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(8.dp))
                    .size(54.dp),
                model = ImageRequest.Builder(LocalContext.current)
                    .data(brand?.logoUrl)
                    .crossfade(true)
                    .crossfade(500)
                    .build(),
                contentDescription = null,
            )
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color = MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "F",
                    style = MaterialTheme.typography.headlineLarge.copy(MaterialTheme.colorScheme.onPrimary)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = brand?.name?.let { "Pay $it" } ?: "",
            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.outline)
        )
        Spacer(modifier = Modifier.height(8.dp))
        AnimatedContent(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            targetState = commerceSession.getAmount(),
            transitionSpec = {
                if (commerceSession.getAmount() > initialState) {
                    (slideInVertically { width -> width } +
                            fadeIn()).togetherWith(slideOutVertically()
                    { width -> -width } + fadeOut())
                } else {
                    (slideInVertically { width -> -width } +
                            fadeIn()).togetherWith(slideOutVertically()
                    { width -> width } + fadeOut())
                }.using(SizeTransform(clip = false))
            }, label = "price_animation"
        ) { state ->
            state
            Text(
                text = commerceSession.getAmountLabel(),
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.W500)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.8F)
                .padding(horizontal = 32.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 24.dp,
                        bottomStart = 24.dp,
                        topEnd = 40.dp,
                        bottomEnd = 40.dp
                    )
                )
        ) {
            val code by remember {
                derivedStateOf {
                    commerceSession?.data?.authorization?.number
                        ?: BuildConfig.LIBRARY_PACKAGE_NAME
                }
            }
            PDF417(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                columns = 2,
                rows = 23,
                code = code
            )
        }
        Spacer(modifier = Modifier.height(26.dp))
        if (!commerceSession?.data?.authorization?.instructions.isNullOrEmpty()) {
            MarkdownText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                text = commerceSession?.data?.authorization?.instructions ?: "",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
            )
        }
        if (!commerceSession?.data?.authorization?.details.isNullOrEmpty()) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 32.dp, top = 10.dp),
                text = commerceSession?.data?.authorization?.details ?: "",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium),
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Preview(backgroundColor = 0xFFEFEFEF, showBackground = true)
@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun LegacyFlexcodePreview() {
    FlexaTheme {
        Box(contentAlignment = Alignment.BottomCenter) {
            Column {
                LegacyFlexcode(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .navigationBarsPadding(),
                    viewModel = SpendViewModel(FakeInteractor()).apply {
                        brand.value = MockFactory.getMockBrand()
                        amount.value = "13.9"
                    },
                    sheetState = rememberModalBottomSheetState(
                        initialValue = ModalBottomSheetValue.Expanded
                    ),
                    toBack = { },
                    toDetails = { }
                )
            }
        }
    }
}
