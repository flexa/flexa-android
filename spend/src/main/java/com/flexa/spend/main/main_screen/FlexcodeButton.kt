package com.flexa.spend.main.main_screen

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flexa.core.entity.AvailableAsset
import com.flexa.core.theme.FlexaTheme
import com.flexa.spend.MockFactory
import com.flexa.spend.R
import com.flexa.spend.hasBalanceRestrictions
import java.math.BigDecimal

@Composable
fun FlexcodeButton(
    modifier: Modifier = Modifier,
    price: String?,
    onClick: () -> Unit
) {
    FilledTonalButton(
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(),
        onClick = { onClick.invoke() }) {
        AnimatedContent(
            targetState = price,
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
                modifier = Modifier.animateContentSize(),
                text = state ?: stringResource(id = R.string.updating),
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W500
                )
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = Icons.Filled.Info,
            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = .5f),
            contentDescription = null
        )
    }
}

@Composable
fun FlexcodeButtonV2(
    modifier: Modifier = Modifier,
    asset: AvailableAsset?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val balance by remember(asset) {
        mutableStateOf(
            asset?.balanceBundle?.totalLabel?.run {
                "$this ${context.getString(R.string.balance)}"
            } ?: context.getString(R.string.updating)
        )
    }
    FilledTonalButton(
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(),
        onClick = { onClick.invoke() }) {
        Column {
            AnimatedContent(
                targetState = asset?.balanceBundle?.total ?: BigDecimal.ZERO,
                transitionSpec = {
                    if (targetState < initialState) {
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
                state
                Text(
                    modifier = Modifier.animateContentSize(),
                    text = balance,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W500,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
            val balanceRestricted by remember(asset) {
                mutableStateOf(asset?.hasBalanceRestrictions() == true)
            }
            AnimatedContent(
                targetState = balanceRestricted,
                transitionSpec = {
                    if (targetState) {
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
                val infiniteAlpha = rememberInfiniteTransition(label = "").animateFloat(
                    initialValue = .5F, targetValue = 1F, infiniteRepeatable(
                        animation = tween(1000),
                        repeatMode = RepeatMode.Reverse
                    ), label = ""
                )
                if (state) {
                    Text(
                        modifier = Modifier.animateContentSize(),
                        text = "${stringResource(R.string.updating)}...",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W500,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = infiniteAlpha.value)
                        )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = Icons.Filled.Info,
            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = .5f),
            contentDescription = null
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun FlexcodeButtonPreview() {
    FlexaTheme {
        Surface {
            Box(modifier = Modifier.padding(8.dp)) {
                FlexcodeButton(price = "\$243.90 Available") { }
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun FlexcodeButtonV2Preview() {
    FlexaTheme {
        Surface {
            Box(modifier = Modifier.padding(8.dp)) {
                FlexcodeButtonV2(
                    asset = MockFactory.getMockSelectedAsset().asset.copy(
                        balanceBundle = MockFactory.getBalanceBundle()
                    )
                ) {}
            }
        }
    }
}
