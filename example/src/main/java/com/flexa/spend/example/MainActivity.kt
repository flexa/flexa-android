package com.flexa.spend.example

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.flexa.core.Flexa
import com.flexa.core.shared.AssetAccount
import com.flexa.core.shared.AvailableAsset
import com.flexa.core.shared.CustodyModel
import com.flexa.core.shared.FlexaClientConfiguration
import com.flexa.core.theme.FlexaTheme
import com.flexa.identity.buildIdentity
import com.flexa.identity.toSha256
import com.flexa.scan.buildScan
import com.flexa.spend.buildSpend
import com.flexa.spend.enterTransition
import com.flexa.spend.exitTransition

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Flexa.init(
            FlexaClientConfiguration(
                context = applicationContext,
                publishableKey = BuildConfig.PUBLISHABLE_KEY,
                theme = FlexaTheme(),
                assetAccounts = arrayListOf(
                    AssetAccount(
                        assetAccountHash = "1".toSha256(),
                        custodyModel = CustodyModel.LOCAL,
                        displayName = "Example Wallet",
                        icon = "https://flexa.network/static/4bbb1733b3ef41240ca0f0675502c4f7/d8419/flexa-logo%403x.png",
                        availableAssets = listOf(
                            AvailableAsset(
                                assetId = "eip155:1/erc20:0xe7ae9b78373d0D54BAC81a85525826Fd50a1E2d3",
                                symbol = "CR",
                                balance = 3.0
                            ),
                            AvailableAsset(
                                assetId = "eip155:11155111/slip44:60",
                                symbol = "ETH",
                                balance = 0.501,
                            ),
                            AvailableAsset(
                                assetId = "bip122:00040fe8ec8471911baa1db1266ea15d/slip44:133",
                                symbol = "ZEC",
                                balance = 10.0,
                            )
                        )
                    )
                ),
                webViewThemeConfig = "{\n" +
                        "    \"android\": {\n" +
                        "        \"light\": {\n" +
                        "            \"backgroundColor\": \"#100e29\",\n" +
                        "            \"sortTextColor\": \"#ed7f60\",\n" +
                        "            \"titleColor\": \"#ffffff\",\n" +
                        "            \"cardColor\": \"#2a254e\",\n" +
                        "            \"borderRadius\": \"15px\",\n" +
                        "            \"textColor\": \"#ffffff\"\n" +
                        "        },\n" +
                        "        \"dark\": {\n" +
                        "            \"backgroundColor\": \"#100e29\",\n" +
                        "            \"sortTextColor\": \"#ed7f60\",\n" +
                        "            \"titleColor\": \"#ffffff\",\n" +
                        "            \"cardColor\": \"#2a254e\",\n" +
                        "            \"borderRadius\": \"15px\",\n" +
                        "            \"textColor\": \"#ffffff\"\n" +
                        "        }\n" +
                        "    }\n" +
                        "}"
            )
        )

        setContent {
            FlexaTheme {
                ExampleNavHost()
            }
        }
        if (savedInstanceState == null) checkIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        checkIntent(intent)
    }

    private fun checkIntent(intent: Intent?) {
        intent?.data?.toString()?.let { deepLink ->
            Flexa.buildSpend().open(this, deepLink)
        }
    }
}

private const val EXAMPLE_DESTINATION = "example"

@Composable
fun ExampleNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = EXAMPLE_DESTINATION,
    ) {
        composable(EXAMPLE_DESTINATION,
            enterTransition = { enterTransition },
            exitTransition = { exitTransition },
            popEnterTransition = { enterTransition },
            popExitTransition = { exitTransition }) { Greeting() }
    }
}

@Composable
fun Greeting() {
    val activity = LocalContext.current.getActivity()
    val minWidth by remember { mutableStateOf(200.dp) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        item {
            Column(
                modifier = Modifier
                    .systemBarsPadding()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Flexa", style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "The global leader in pure-digital payments",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        item { Spacer(modifier = Modifier.height(30.dp)) }
        item {
            OutlinedButton(
                modifier = Modifier.defaultMinSize(minWidth = minWidth),
                onClick = {
                    activity?.let {
                        Flexa.buildIdentity().open(it)
                    }
                }) {
                Icon(imageVector = Icons.Rounded.AccountCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Login")
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item {
            OutlinedButton(
                modifier = Modifier.defaultMinSize(minWidth = minWidth),
                onClick = {
                    activity?.let {
                        Flexa.buildScan()
                            .onSendCallback { result ->
                                if (result.isSuccess) {
                                    Log.d(null, "onCodeScanned: ${result.getOrThrow()}")
                                }
                            }.open(it)
                    }
                }) {
                Icon(imageVector = Icons.Rounded.QrCode2, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Scan")
            }
        }
        item {
            OutlinedButton(
                modifier = Modifier.defaultMinSize(minWidth = minWidth),
                onClick = {
                    activity?.let {
                        Flexa.buildSpend()
                            .onPaymentAuthorization { pa ->
                                Log.d(null, "onPaymentAuthorization: $pa")
                            }
                            .onTransactionRequest { result ->
                                when {
                                    result.isSuccess -> {
                                        Log.d(null, "onTransactionRequest: ${result.getOrNull()}")
                                    }
                                    result.isFailure -> {
                                        Log.d(null, "onTransactionRequest:", result.exceptionOrNull())
                                    }
                                }
                            }
                            .open(it)
                    }
                }) {
                Icon(imageVector = Icons.Rounded.ShoppingCart, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Pay")
            }
        }
        item {
            Spacer(
                modifier = Modifier
                    .height(30.dp)
                    .systemBarsPadding()
            )
        }
    }
}

@Preview
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun DefaultPreview() {
    FlexaTheme {
        Greeting()
    }
}

fun Context.getActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}
