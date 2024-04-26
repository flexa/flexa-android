package com.flexa.core

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.flexa.BuildConfig
import com.flexa.R
import com.flexa.core.data.data.AppInfoProvider
import com.flexa.core.entity.Account
import com.flexa.core.entity.AvailableAsset
import com.flexa.core.shared.AppAccount
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

fun NavController.navigate(route: String, vararg params: Pair<String, String>) {
    val urlBuilder = StringBuilder()
    urlBuilder.append(route)
    for (param in params) {
        urlBuilder.append(param.first)
        urlBuilder.append(param.second.toNavArgument())
    }
    navigate(urlBuilder.toString())
}

fun String.toNavArgument(): String = try {
    URLEncoder.encode(this, StandardCharsets.UTF_8.name())
} catch (e: java.lang.Exception) {
    ""
}

@Composable
fun Lifecycle.observeAsState(): State<Lifecycle.Event> {
    val state = remember { mutableStateOf(Lifecycle.Event.ON_ANY) }
    DisposableEffect(this) {
        val observer = LifecycleEventObserver { _, event ->
            state.value = event
        }
        this@observeAsState.addObserver(observer)
        onDispose {
            this@observeAsState.removeObserver(observer)
        }
    }
    return state
}

fun Instant.minutesBetween(timestamp: Long): Long {
    runCatching {
        Duration.between(this, Instant.ofEpochMilli(timestamp * 1000L))
    }.fold(
        onSuccess = { return it.toMinutes() },
        onFailure = { return 0 }
    )
}

fun List<AppAccount>.toJsonObject(): JsonObject =
    buildJsonObject {
        putJsonArray("data") {
            forEach { acc ->
                add(acc.toJsonObject())
            }
        }
    }

fun AppAccount.toJsonObject(): JsonObject =
    buildJsonObject {
        put("account_id", accountId)
        displayName?.let { put("display_name", it) }
        icon?.let { put("icon", it) }
        if (availableAssets.isNotEmpty()) {
            putJsonArray("assets") {
                availableAssets.forEach { asset ->
                    addJsonObject {
                        put("asset", asset.assetId)
                        put("balance", asset.balance.toString())
                        asset.icon?.let { put("icon", it) }
                        asset.displayName?.let { put("display_name", it) }
                        asset.symbol?.let { put("symbol", it) }
                    }
                }
            }
        }
    }


fun String.toDate(dateFormat: String = "EEE, dd MMM yyyy HH:mm:ss z"): Date =
    try {
        SimpleDateFormat(dateFormat, Locale.US).parse(this) ?: Date()
    } catch (e: ParseException) {
        Log.e("TAG", "toDate:", e)
        Date()
    }

fun AvailableAsset.zeroValue(): Boolean {
    return if (this.value == null) {
        true
    } else {
        val regex = Regex(pattern = """\d+\.\d{2}""")
        val matchResult = regex.find(this.value.label)
        val extractedValue = matchResult?.value
        extractedValue?.toDoubleOrNull() == 0.0
    }
}

fun com.flexa.core.entity.AppAccount.nonZeroAssets(): List<AvailableAsset> =
    availableAssets.filter { !it.zeroValue() }

fun Activity?.sendFlexaReport(data: String? = null) {
    this?.run {
        val messageBody = StringBuilder()
        messageBody.append(AppInfoProvider.getAppName(this.application))
        messageBody.append(" ")
        messageBody.append(AppInfoProvider.getAppVersion(this.application))
        messageBody.append(" ${AppInfoProvider.getAppPackageName(this.application)} ")

        val deviceModel = Build.MODEL
        val deviceManufacturer = Build.MANUFACTURER
        val userAgent =
            "$deviceManufacturer $deviceModel/${Build.VERSION.SDK_INT}(${Build.VERSION.RELEASE}) " +
                    "Flexa/${BuildConfig.SPEND_SDK_VERSION} "
        messageBody.append("\n• $userAgent")
        data?.let { messageBody.append("\n$it\n") }
        messageBody.append("\n\n")

        val uriText = "mailto:${getString(R.string.flexa_report_email)}" +
                "?subject=" + Uri.encode(getString(R.string.report_an_issue)) +
                "&body=" + Uri.encode(messageBody.toString())
        val uri: Uri = Uri.parse(uriText)
        val sendIntent = Intent(Intent.ACTION_SENDTO)
        sendIntent.data = uri
        try {
            startActivity(Intent.createChooser(sendIntent, getString(R.string.send_email)))
        } catch (ex: ActivityNotFoundException) {
            Log.e(null, ex.message, ex)
        }
    }
}

fun Account.hasLimits(): Boolean {
    return !this.limits.isNullOrEmpty()
}

fun Account.getLimitsPercentage(): Float {
    if (limits.isNullOrEmpty()) {
        return 0F
    } else {
        val limit = limits.firstOrNull()
        val overall = limit?.amount?.toFloatOrNull() ?: 0F
        val remaining = limit?.remaining?.toFloatOrNull() ?: 0F
        return remaining / overall
    }
}

fun Account.getJoinedYear(): String? {
    return if (this.created != null) {
        val timestamp = this.created
        val dateTime =
            LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault())
        val year = dateTime.year
        year.toString()
    } else {
        null
    }
}

fun Account.getResetDay(): String {
    val timestamp = this.limits?.firstOrNull()?.resetsAt ?: Instant.now().toEpochMilli()
    val dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("EEEE")
    val formattedDateTime = dateTime.format(formatter)
    return formattedDateTime
}

fun Account.getResetHour(): String {
    val timestamp = this.limits?.firstOrNull()?.resetsAt ?: Instant.now().toEpochMilli()
    val dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("h:mm a")
    val formattedDateTime = dateTime.format(formatter)
    return formattedDateTime
}
