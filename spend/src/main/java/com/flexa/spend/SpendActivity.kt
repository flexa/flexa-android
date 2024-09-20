package com.flexa.spend

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.flexa.core.Flexa
import com.flexa.core.shared.FlexaConstants
import com.flexa.core.theme.FlexaTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SpendActivity : ComponentActivity(), ImageLoaderFactory {

    private val broadcastReceiver = SpendBroadcastReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceiver()
        restoreSDKContext()
        setContent {
            FlexaTheme {
                PayNavHost(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    deepLink = intent?.getStringExtra(KEY_DEEP_LINK)
                )
            }
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(broadcastReceiver) }
        super.onDestroy()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .build()
    }

    private fun restoreSDKContext() {
        if (Flexa.context == null) {
            Flexa.context = applicationContext
        }
    }

    private fun registerReceiver() {
        Flexa.scope.launch {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(
                    broadcastReceiver,
                    IntentFilter(Spend.interactor.getPublishableKey()),
                )
            } else {
                registerReceiver(
                    broadcastReceiver,
                    IntentFilter(Spend.interactor.getPublishableKey()),
                    RECEIVER_EXPORTED
                )
            }
        }
    }

    companion object {
        internal const val KEY_DEEP_LINK = "deep_link"
    }

    private class SpendBroadcastReceiver : BroadcastReceiver() {

        private var job: Job? = null

        override fun onReceive(context: Context, intent: Intent) {
            val valid = intent.getBooleanExtra(FlexaConstants.TOKEN, false)
            updateTokenState(valid)
        }
        private fun updateTokenState(valid: Boolean) {
            job?.cancel()
            job = Flexa.scope.launch {
                if (valid) Spend.tokenState.value = TokenState.Fine
                else {
                    if (isActive)
                        Spend.tokenState.value = TokenState.Error
                }
            }
        }
    }
}

@Composable
fun PayNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    deepLink: String? = null
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = PAY_ROUTE,
    ) {
        spendNavGraph(
            modifier = modifier,
            navController = navController,
            deepLink = deepLink
        )
    }
}
