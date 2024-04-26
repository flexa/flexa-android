package com.flexa.spend.main.main_screen

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Picture
import android.graphics.RectF
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGImageView
import com.flexa.core.shared.Brand
import com.flexa.core.view.FlexaProgress
import com.flexa.spend.R
import com.flexa.spend.blur
import com.flexa.spend.merchants.BrandsViewModel
import java.nio.charset.StandardCharsets

@Composable
internal fun BrandsRow(
    modifier: Modifier = Modifier,
    viewModel: BrandsViewModel,
    toEdit: () -> Unit,
    onClick: (Brand) -> Unit = {}
) {
    val horizontalPadding: Dp = 18.dp
    val brands by remember { derivedStateOf { viewModel.itemsA + viewModel.itemsB } }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = horizontalPadding + 8.dp, end = horizontalPadding,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.more_instant_payments),
                fontWeight = FontWeight.W600,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = { toEdit.invoke() }) {
                Icon(
                    modifier = Modifier.size(22.dp),
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                    tint =  MaterialTheme.colorScheme.primary
                )
            }
        }
        AnimatedVisibility(visible = brands.isEmpty()) {
            FlexaProgress(
                modifier = Modifier
                    .size(114.dp)
                    .padding(30.dp)
                    .alpha(0F),
                roundedCornersSize = 12.dp,
            )
        }
        AnimatedVisibility(visible = brands.isNotEmpty()) {
            LazyRow {
                brands.forEachIndexed { index, item ->
                    item {
                        if (index == 0) Spacer(modifier = Modifier.width(horizontalPadding))
                        BrandRowItem(
                            Modifier.height(114.dp),
                            item.brand,
                            onClick = { onClick.invoke(it) }
                        )
                        if (index == brands.size - 1) Spacer(
                            modifier = Modifier.width(
                                horizontalPadding
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BrandRowItem(
    modifier: Modifier = Modifier,
    item: Brand,
    onClick: (Brand) -> Unit = {}
) {
    TextButton(
        modifier = modifier,
        onClick = { onClick.invoke(item) }) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            AsyncImage(
                modifier = Modifier
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(6.dp))
                    .clip(RoundedCornerShape(6.dp))
                    .size(54.dp),
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.logoUrl)
                    .crossfade(true)
                    .crossfade(500)
                    .build(),
                contentDescription = null,
                error = painterResource(id = com.flexa.R.drawable.ic_flexa)
            )
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .weight(1f)
            ) {
                Text(
                    modifier = Modifier.align(Center),
                    text = item.name,
                    maxLines = 2,
                    style = TextStyle(
                        fontWeight = FontWeight.W600,
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline
                    ),
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun PayFeeButton(
    modifier: Modifier = Modifier,
    price: String,
    onClick: () -> Unit,
) {
    Column(modifier = modifier) {
        TextButton(
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD9D9D9)
            ),
            onClick = { onClick.invoke() }) {
            Text(
                text = "\$$price network fee",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W500,
                    color = Color.Black.copy(alpha = .5f)
                )
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = Icons.Rounded.Info,
                tint = Color.Black.copy(alpha = .5f),
                contentDescription = null
            )
        }
        Spacer(modifier = Modifier.systemBarsPadding())
    }
}

@Composable
fun FlexcodeCap(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val flexcode by remember {
        mutableStateOf(
            try {
                val code =
                    context.assets.open("flexcode_svg.txt")
                        .bufferedReader().use { it.readText() }
                        .toByteArray()
                val svgAsString = String(code, StandardCharsets.UTF_8)
                val svg = SVG.getFromString(svgAsString)
                svg
            } catch (e: Exception) {
                null
            }
        )
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            SVGImageView(ctx).apply { runCatching { setSVG(flexcode) } }
        })
}

@Composable
fun FlexcodeCapAPI30(
    modifier: Modifier = Modifier,
    svgPadding: Dp = 0.dp,
    blurRadius: Float = 0F
) {
    val context = LocalContext.current
    val svg by remember {
        mutableStateOf(
            try {
                val code =
                    context.assets.open("flexcode_svg.txt")
                        .bufferedReader().use { it.readText() }
                        .toByteArray()
                val svgAsString = String(code, StandardCharsets.UTF_8)
                SVG.getFromString(svgAsString)
            } catch (e: Exception) {
                null
            }
        )
    }
    FlexcodeAPI30(
        modifier = modifier,
        svg = svg,
        svgPadding = svgPadding,
        blurRadius = blurRadius
    )
}

@Composable
fun FlexcodeAPI30(
    modifier: Modifier = Modifier,
    svg: SVG?,
    svgPadding: Dp = 0.dp,
    blurRadius: Float = 0F
) {
    val context = LocalContext.current
    var pxSvgWidth by remember { mutableStateOf(0F) }
    var pxSvgHeight by remember { mutableStateOf(0F) }
    val padding = with(LocalDensity.current) { svgPadding.toPx() }
    val flexcode by remember(svg, blurRadius, pxSvgHeight) {
        mutableStateOf(
            try {
                val svgPicture = svg?.apply {
                    documentWidth = pxSvgWidth
                    documentHeight = pxSvgHeight
                }?.renderToPicture()
                val bitmap =
                    Bitmap.createBitmap(
                        pxSvgWidth.toInt(), pxSvgHeight.toInt(),
                        Bitmap.Config.ARGB_8888
                    )
                val canvas = Canvas(bitmap)
                canvas.drawPicture(
                    svgPicture ?: Picture(),
                    RectF(
                        padding,
                        padding,
                        ((svg?.documentWidth ?: 0F) - padding).coerceAtLeast(padding),
                        ((svg?.documentHeight ?: 0F) - padding).coerceAtLeast(padding)
                    )
                )
                val res =
                    if (blurRadius > 0) bitmap.blur(context, blurRadius)
                    else bitmap
                res.asImageBitmap()
            } catch (e: Exception) {
                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap()
            }
        )
    }
    Image(
        modifier = modifier
            .onGloballyPositioned {
                pxSvgWidth = it.size.width.toFloat()
                pxSvgHeight = it.size.height.toFloat()
            },
        painter = BitmapPainter(flexcode), contentDescription = null
    )
}

@Preview
@Preview(name = "dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun MerchantItemPreview() {
    BrandRowItem(
        modifier = Modifier.height(114.dp),
        item = Brand(
            "", "", "","",
            emptyList(), "https://flexa.network/static/4bbb1733b3ef41240ca0f0675502c4f7/d8419/flexa-logo%403x.png", "Flexa", "", ""
        ),
        onClick = {  }
    )
}

