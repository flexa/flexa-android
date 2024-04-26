package com.flexa.spend.main.assets

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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

@Composable
fun NavigationDrawer() {
    Column( // Bottom Sheet Navigation Drawer
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val color = MaterialTheme.colorScheme.outline.copy(alpha = .5F)
        Spacer(modifier = Modifier.height(8.dp))
        Canvas(modifier = Modifier
            .width(58.dp)
            .height(6.dp),
            onDraw = {
                drawLine(
                    start = Offset(x = 0f, y = (6.dp / 2).toPx()),
                    end = Offset(x = 58.dp.toPx(), y = (6.dp / 2).toPx()),
                    color = color,
                    strokeWidth = 8.dp.toPx(),
                    cap = StrokeCap.Round, //add this line for rounded edges
                )
            })
        Spacer(modifier = Modifier.height(8.dp))
    }
}


@Composable
fun AssetsSheetHeader(
    title: String,
    toSettings: () -> Unit = {}
) {
    val palette = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            modifier = Modifier.padding(start = 14.dp),
            text = title,
            style = TextStyle(
                color = palette.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.W400
            )
        )
        IconButton(
            onClick = { toSettings() }) {
            Icon(
                modifier = Modifier.size(21.dp),
                imageVector = Icons.Outlined.Settings,
                tint = palette.onSurfaceVariant,
                contentDescription = null
            )
        }
    }
}

@Composable
fun AssetsSettingsSheetHeader(
    toBack: () -> Unit = {}
) {
    val palette = MaterialTheme.colorScheme
    BackHandler { toBack() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = { toBack() }) {
            Icon(
                modifier = Modifier.size(21.dp),
                imageVector = Icons.Outlined.ArrowBack,
                tint = palette.onSurfaceVariant,
                contentDescription = null
            )
        }
        Text(
            modifier = Modifier
                .padding(start = 14.dp)
                .weight(1F, true),
            text = stringResource(id = R.string.settings),
            style = TextStyle(
                color = palette.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.W400
            )
        )
    }
}

@Composable
fun AssetInfoSheetHeader(
    modifier: Modifier = Modifier,
    asset: AvailableAsset,
    toSettings: () -> Unit = {},
    toBack: () -> Unit = {},
    backNavigation: Boolean = false
) {
    val palette = MaterialTheme.colorScheme
    BackHandler(backNavigation) { toBack() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (backNavigation) {
            IconButton(
                onClick = { toBack() }) {
                Icon(
                    modifier = Modifier.size(21.dp),
                    imageVector = Icons.Outlined.ArrowBack,
                    tint = palette.onSurfaceVariant,
                    contentDescription = null
                )
            }
        }
        Text(
            modifier = Modifier
                .padding(start = 14.dp)
                .weight(1F, true),
            text = asset.assetData?.displayName?:"",
            style = TextStyle(
                color = palette.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        )
        if (backNavigation) {
            IconButton(
                onClick = { toSettings() }) {
                Icon(
                    modifier = Modifier.size(21.dp),
                    imageVector = Icons.Outlined.Settings,
                    tint = palette.onSurfaceVariant,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
fun AssetsSheetStickyHeader(
    modifier: Modifier = Modifier,
    text: String,
    color: Color
) {
    val palette = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = TextStyle(
                color = palette.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.W500
            )
        )
    }
}

@Composable
fun HideShortBalances(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    val palette = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(30.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(15.dp))
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.hide_short_balances),
                style = TextStyle(
                    fontSize = 17.sp, fontWeight = FontWeight.W500,
                    color = palette.onBackground
                )
            )
            androidx.compose.material3.Switch(
                colors = androidx.compose.material3.SwitchDefaults.colors(),
                checked = checked, onCheckedChange = {
                    onChecked.invoke(it)
                },
                thumbContent = {}
            )
        }
        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Preview
@Composable
private fun AssetsSheetStickyHeaderPreview() {
    FlexaTheme {
        AssetsSheetStickyHeader(
            text = "Flexa wallet",
            color = MaterialTheme.colorScheme.background
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AssetsSheetFooterPreview() {
    FlexaTheme {
        HideShortBalances(
            modifier = Modifier.fillMaxWidth(),
            checked = false,
            onChecked = {

            }
        )
    }
}

@Preview(
    showBackground = true,
)
@Composable
private fun AssetsSheetHeaderPreview() {
    FlexaTheme {
        AssetsSheetHeader(title = stringResource(id = R.string.pay_using))
    }
}

@Preview(
    showBackground = true,
)
@Composable
private fun AssetsSettingsSheetHeaderPreview() {
    FlexaTheme {
        AssetsSettingsSheetHeader()
    }
}

@Preview(showBackground = true)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun AssetInfoSheetHeaderPreview() {
    FlexaTheme {
        AssetInfoSheetHeader(
            asset = MockFactory.getMockSelectedAsset().asset
        )
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL,
    showSystemUi = false,
    showBackground = true
)
@Composable
private fun NavigationDrawerPreview() {
    FlexaTheme {
        NavigationDrawer()
    }
}

