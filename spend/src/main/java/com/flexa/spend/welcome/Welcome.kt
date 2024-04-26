package com.flexa.spend.welcome

import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.flexa.core.Flexa
import com.flexa.core.observeAsState
import com.flexa.core.theme.FlexaTheme
import com.flexa.identity.buildIdentity
import com.flexa.identity.shared.ConnectResult
import com.flexa.spend.R

@Composable
fun Welcome(
    modifier: Modifier = Modifier,
    toStoresAndRestaurants: () -> Unit,
    toLogin: () -> Unit,
    toBack: () -> Unit,
    toPay: () -> Unit
) {

    val palette = MaterialTheme.colorScheme
    val density = LocalDensity.current
    var bottomButtonsHeight by remember {
        mutableStateOf(120.dp)
    }
    val lifecycleState = LocalLifecycleOwner.current.lifecycle.observeAsState()
    val state = lifecycleState.value
    val previewMode = LocalInspectionMode.current
    val appName by remember {
        if (!previewMode) {
            val context = Flexa.requiredContext
            mutableStateOf(
                try {
                    val packageInfo = when {
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ->
                            Flexa.requiredContext.packageManager
                                .getPackageInfo(context.packageName, 0)

                        else -> context.packageManager
                            .getPackageInfo(
                                context.packageName,
                                PackageManager.PackageInfoFlags.of(0)
                            )
                    }
                    context.getString(packageInfo.applicationInfo.labelRes)
                } catch (e: Exception) {
                    "Flexa"
                }
            )
        } else {
            mutableStateOf("Flexa")
        }
    }

    LaunchedEffect(state) {
        Flexa.buildIdentity().build().collect {
            if (it is ConnectResult.Connected) toPay.invoke()
        }
    }

    Box(
        modifier = modifier
            .statusBarsPadding()
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(color = palette.background),
    ) {
        Box(
            modifier = Modifier
                .padding(10.dp)
                .zIndex(1f)
                .align(Alignment.TopEnd)
        ) {
            IconButton(
                onClick = { toBack.invoke() }) {
                Icon(
                    modifier = Modifier.size(30.dp),
                    imageVector = Icons.Rounded.Cancel,
                    tint = palette.onBackground,
                    contentDescription = null
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .padding(start = 42.dp, end = 42.dp, bottom = bottomButtonsHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = "$appName invites you to",
                style = TextStyle(
                    color = palette.outline,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.W600
                )
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(id = R.string.pay_with_flexa),
                style = TextStyle(
                    color = palette.onBackground,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.W600
                )
            )
            Spacer(modifier = Modifier.height(40.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Canvas(modifier = Modifier.size(36.dp),
                    onDraw = { drawCircle(color = palette.outlineVariant) })
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier.padding(start = 16.dp),
                        text = "Thousands of places to pay",
                        style = TextStyle(
                            fontWeight = FontWeight.W600,
                            fontSize = 16.sp,
                            color = palette.onBackground
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        modifier = Modifier.padding(start = 16.dp),
                        text = "Instantly spend your Wallet balances anywhere Flexa is accepted.",
                        style = TextStyle(
                            fontWeight = FontWeight.W400,
                            fontSize = 12.sp,
                            color = palette.outline
                        )
                    )
                    TextButton(
                        modifier = Modifier.padding(start = 4.dp),
                        onClick = { toStoresAndRestaurants.invoke() }) {
                        Text(
                            text = "Browse stores and restaurants",
                            style = TextStyle(
                                fontWeight = FontWeight.W400,
                                fontSize = 12.sp,
                                color = palette.primary,
                                letterSpacing = 1.sp
                            )
                        )
                        Box(modifier = Modifier.padding(top = 2.dp)) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                imageVector = Icons.Rounded.ChevronRight,
                                tint = palette.primary,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Canvas(modifier = Modifier.size(36.dp),
                    onDraw = { drawCircle(color = palette.outlineVariant) })
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier.padding(start = 16.dp, end = 30.dp),
                        text = "Secure and private by design",
                        style = TextStyle(
                            fontWeight = FontWeight.W600,
                            fontSize = 16.sp,
                            color = palette.onBackground
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        modifier = Modifier.padding(start = 16.dp, end = 30.dp),
                        text = "Spend confidently. Flexa payments are designed to keep your personal data safe and out of the hands of harvesters.",
                        style = TextStyle(
                            fontWeight = FontWeight.W400,
                            fontSize = 12.sp,
                            color = palette.outline
                        )
                    )
                }
            }
        }
        BottomContent(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .statusBarsPadding()
                .padding(paddingValues = PaddingValues(34.dp))
                .onGloballyPositioned {
                    bottomButtonsHeight = density.run { it.size.height.toDp() }
                },
            toLogin = { toLogin.invoke() },
            toBack = { toBack.invoke() }
        )
    }
}

@Composable
private fun BottomContent(
    modifier: Modifier = Modifier,
    toLogin: () -> Unit,
    toBack: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            onClick = { toLogin.invoke() }) {
            Text(
                text = stringResource(id = R.string.get_started),
                style = TextStyle(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.W600,
                )
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            modifier = Modifier.systemBarsPadding(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            onClick = { toBack.invoke() }) {
            Text(
                text = stringResource(id = R.string.maybe_later),
                style = TextStyle(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.W500,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    }

}

@Preview(name = "Light")
@Preview(name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun WelcomePreview() {
    FlexaTheme {
        Surface {
            Welcome(
                modifier = Modifier.fillMaxSize(),
                toStoresAndRestaurants = {},
                toLogin = {},
                toBack = {},
                toPay = {}
            )
        }
    }
}
