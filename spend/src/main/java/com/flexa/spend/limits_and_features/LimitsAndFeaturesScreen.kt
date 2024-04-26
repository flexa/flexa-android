package com.flexa.spend.limits_and_features

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flexa.core.theme.FlexaTheme
import com.flexa.spend.R

@Composable
fun LimitsAndFeaturesScreen(
    modifier: Modifier = Modifier,
    toBack: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .statusBarsPadding()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(color = MaterialTheme.colorScheme.background)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        LimitsAndFeaturesHeader(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
        ) { toBack?.invoke() }
        Column(
            modifier = modifier.verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            LimitsAndFeaturesLocation(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(color = Color.White)
                    .clickable { }
            )
            Spacer(modifier = Modifier.height(24.dp))
            SpendAndLoad(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(color = Color.White, shape = RoundedCornerShape(16.dp))
            )
            Spacer(modifier = Modifier.height(30.dp))
            Text(
                modifier = Modifier.padding(horizontal = 20.dp),
                text = "Payments from Wallet".uppercase(),
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.W500,
                    fontSize = 13.sp
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            DefaultAssetMenu(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(color = Color.White, shape = RoundedCornerShape(16.dp))
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                modifier = Modifier.padding(horizontal = 20.dp),
                text = stringResource(id = R.string.limits_and_features_footer),
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.W400,
                    fontSize = 13.sp
                )
            )
            Spacer(modifier = Modifier.height(30.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun LimitsAndFeaturesHeader(
    modifier: Modifier = Modifier,
    toBack: (() -> Unit)? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val palette = MaterialTheme.colorScheme
        val density = LocalDensity.current
        var buttonWidth by remember { mutableStateOf(60.dp) }
        TextButton(
            modifier = Modifier.onGloballyPositioned {
                buttonWidth = density.run { it.size.width.toDp() }
            },
            onClick = { toBack?.invoke() }) {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = Icons.Rounded.ArrowBackIosNew,
                tint = palette.onBackground,
                contentDescription = null
            )
            Text(
                text = stringResource(id = R.string.back),
                style = TextStyle(color = palette.onBackground, fontWeight = FontWeight.W600)
            )
        }
        Text(
            text = stringResource(id = R.string.limits_and_features),
            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.W600, color = palette.onBackground)
        )
        Spacer(modifier = Modifier.width(buttonWidth))
    }
}

@Composable
private fun LimitsAndFeaturesLocation(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                modifier = Modifier.size(30.dp),
                imageVector = Icons.Rounded.Public,
                tint = Color.Black,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(id = R.string.location),
                style = TextStyle(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.W500,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "United States",
                style = TextStyle(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.W500,
                    color = Color.Black.copy(alpha = .5F)
                )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                modifier = Modifier.size(30.dp),
                imageVector = Icons.Rounded.UnfoldMore,
                tint = Color.Black.copy(alpha = .5F),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
    }
}

@Composable
private fun SpendAndLoad(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Column(modifier = Modifier
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .clickable {

            }) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = Icons.Rounded.OpenInNew,
                        tint = Color.Black,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(id = R.string.spend),
                        style = TextStyle(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.W600,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                Text(
                    modifier = Modifier.padding(end = 2.dp),
                    text = "\$750/week",
                    style = TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.W400,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 50.dp, end = 18.dp),
                text = stringResource(id = R.string.make_payments_using_digital_currency),
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.W400,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Divider(color = Color.White, modifier = Modifier.width(50.dp), thickness = .5.dp)
            Divider(
                color = Color.Black.copy(alpha = .2F),
                modifier = Modifier.fillMaxWidth(),
                thickness = .5.dp
            )
        }
        Column(modifier = Modifier
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .clickable {

            }) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = Icons.Rounded.SystemUpdateAlt,
                        tint = Color.Black,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(id = R.string.load),
                        style = TextStyle(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.W600,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                Text(
                    modifier = Modifier.padding(end = 2.dp),
                    text = stringResource(id = R.string.coming_soon),
                    style = TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.W400,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 50.dp, end = 18.dp),
                text = stringResource(id = R.string.buy_digital_currency_in_person),
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.W400,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun DefaultAssetMenu(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Column(modifier = Modifier
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .clickable {

            }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(id = R.string.default_asset),
                    style = TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.W500,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        text = stringResource(id = R.string.last_used),
                        style = TextStyle(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.W500,
                            color = Color.Black.copy(alpha = .5F)
                        )
                    )
                    Icon(
                        modifier = Modifier.size(30.dp),
                        imageVector = Icons.Rounded.UnfoldMore,
                        tint = Color.Black.copy(alpha = .5F),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Divider(color = Color.White, modifier = Modifier.width(16.dp), thickness = .5.dp)
            Divider(
                color = Color.Black.copy(alpha = .2F),
                modifier = Modifier.fillMaxWidth(),
                thickness = .5.dp
            )
        }
        Column(modifier = Modifier
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .clickable {

            }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(id = R.string.review_before_signing),
                    style = TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.W500,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        text = stringResource(id = R.string.always),
                        style = TextStyle(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.W500,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Icon(
                        modifier = Modifier.size(30.dp),
                        imageVector = Icons.Rounded.ChevronRight,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }
        }
    }
}


@Preview
@Composable
private fun LimitsAndFeaturesPreview() {
    FlexaTheme {
        Surface {
            LimitsAndFeaturesScreen(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colorScheme.surface)
            )
        }
    }
}
