package com.flexa.spend.main.main_screen

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flexa.core.theme.FlexaTheme
import com.flexa.spend.R

@OptIn(ExperimentalAnimationApi::class)
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
            state
            Text(
                modifier = Modifier.animateContentSize(),
                text = price ?: stringResource(id = R.string.updating),
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

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun FlexcodeButtonPreview() {
    FlexaTheme {
        Surface {
            FlexcodeButton(price = "\$243.90 Available") {

            }
        }
    }
}
