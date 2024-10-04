package com.flexa.spend

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.IntOffset
import com.flexa.core.data.db.BrandSession
import com.flexa.core.entity.AppAccount
import com.flexa.core.entity.AssetKey
import com.flexa.core.entity.AvailableAsset
import com.flexa.core.entity.CommerceSession
import com.flexa.core.entity.ExchangeRate
import com.flexa.core.shared.SelectedAsset
import com.flexa.core.toCurrencySign
import com.flexa.identity.getActivity
import com.flexa.spend.data.totp.HmacAlgorithm
import com.flexa.spend.data.totp.TimeBasedOneTimePasswordConfig
import com.flexa.spend.data.totp.TimeBasedOneTimePasswordGenerator
import com.flexa.spend.main.main_screen.SpendViewModel
import com.flexa.spend.merchants.BrandListItem
import com.flexa.spend.merchants.SlideState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.apache.commons.codec.binary.Base32
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlin.math.sign

internal fun String?.toColor(): Color {
    return if (this.isNullOrEmpty()) Color.Gray else
        try {
            Color(android.graphics.Color.parseColor(this))
        } catch (e: IllegalArgumentException) {
            Color.Gray
        }
}

internal fun Color.toCssRgba(): String =
    "rgba(${(red * 255).toInt()}, ${(green * 255).toInt()}, ${(blue * 255).toInt()}," +
            " ${BigDecimal.valueOf(alpha.toDouble()).round(MathContext(2))})"

internal fun List<AppAccount>.getKey(asset: SelectedAsset?): AssetKey? {
    return when {
        asset == null -> null
        asset.asset.key != null -> asset.asset.key
        else -> {
            this.flatMap { it.availableAssets }
                .firstOrNull { it.key != null && it.livemode == asset.asset.livemode }
                ?.key ?: if (asset.asset.livemode == true)
                SpendViewModel.livemodeAsset?.key
            else SpendViewModel.testmodeAsset?.key
        }
    }
}

internal fun SelectedAsset.isSelected(accountId: String, assetId: String): Boolean {
    return this.accountId == accountId && this.asset.assetId == assetId
}

internal fun String?.getAmount(): BigDecimal {
    val regex = """\d+(\.\d+)?""".toRegex()
    return regex.find(this ?: "0")?.value?.toBigDecimalOrNull() ?: BigDecimal.ZERO
}

internal fun String?.getDigitWithPrecision(precision: Int): String {
    val value = this.getAmount()
    val scaledValue = value.setScale(precision, RoundingMode.DOWN).stripTrailingZeros()
    return scaledValue.toPlainString()
}

internal fun AvailableAsset.logo(): String? =
    if (this.icon?.isNotBlank() == true)
        this.icon else this.assetData?.iconUrl

internal fun CommerceSession.label(): String? =
    this.data?.debits?.firstOrNull { it?.label != null }?.label

internal fun CommerceSession.transaction(): CommerceSession.Data.Transaction? =
    this.data?.transaction()

@Composable
fun rememberTOTP(secret: String, length: Int): State<TimeBasedOneTimePasswordGenerator> {
    return produceState(
        initialValue = TimeBasedOneTimePasswordGenerator(
            Base32().decode(secret),
            TimeBasedOneTimePasswordConfig(
                codeDigits = length,
                hmacAlgorithm = HmacAlgorithm.SHA1,
                timeStep = 30,
                timeStepUnit = TimeUnit.SECONDS
            )
        ),
        secret, length
    ) {
        val totp = TimeBasedOneTimePasswordGenerator(
            Base32().decode(secret),
            TimeBasedOneTimePasswordConfig(
                codeDigits = length,
                hmacAlgorithm = HmacAlgorithm.SHA1,
                timeStep = 30,
                timeStepUnit = TimeUnit.SECONDS
            )
        )
        value = totp
    }
}

fun CommerceSession.isCompleted(): Boolean {
    return this.data?.isCompleted() == true
}

fun CommerceSession.Data?.isCompleted(): Boolean {
    return this?.status == "completed"
}

fun CommerceSession?.isValid(): Boolean {
    return this?.data?.isValid() ?: false
}

fun CommerceSession.Data?.isValid(): Boolean {
    val nonNull = this != null
    val rightStatus = this?.status != null && this.status != "closed"
    val transaction = this?.transactions?.firstOrNull {
        it?.status == "requested" ||
                it?.status == "approved"
    }
    val notExpired = transaction?.notExpired() ?: false
    return nonNull && rightStatus && transaction != null && notExpired
}

fun CommerceSession.Data?.transaction(): CommerceSession.Data.Transaction? {
    return this?.transactions?.firstOrNull {
        it?.status == "requested" ||
                it?.status == "approved"
    }
}

fun CommerceSession.Data.Transaction?.notExpired(): Boolean {
    return if (this == null) {
        false
    } else {
        val expiresAt = this.expiresAt ?: 0
        val currentTimestamp = System.currentTimeMillis() / 1000L
        expiresAt > currentTimestamp
    }
}

fun CommerceSession.Data?.hasAnotherAsset(assetId: String): Boolean {
    return this?.transaction()?.asset != assetId
}

fun CommerceSession?.containsAuthorization() =
    if (this == null) {
        false
    } else {
        this.data?.authorization != null &&
                this.data?.authorization?.number != null
    }

fun CommerceSession?.getAmount(): BigDecimal {
    return this?.data?.amount?.toBigDecimalOrNull() ?: BigDecimal.ZERO
}

fun CommerceSession?.getAmountLabel(): String {
    return this?.data?.debits?.firstOrNull { !it?.amount.isNullOrEmpty() }?.label ?: ""
}

fun CommerceSession?.toTransaction(): Transaction? {
    return if (this == null) {
        null
    } else {
        val currentTransaction = this.data?.transactions?.firstOrNull {
            it?.status == "requested" ||
                    it?.status == "approved"
        }
        Transaction(
            commerceSessionId = this.data?.id ?: "",
            amount = currentTransaction?.amount ?: "",
            appAccountId = Spend.selectedAsset.value?.accountId ?: "",
            assetId = currentTransaction?.asset ?: "",
            destinationAddress = currentTransaction?.destination?.address ?: "",
            feeAmount = currentTransaction?.fee?.amount ?: "",
            feeAssetId = currentTransaction?.fee?.asset ?: "",
            feePrice = currentTransaction?.fee?.price?.amount ?: "",
            feePriorityPrice = currentTransaction?.fee?.price?.priority ?: "",
            size = currentTransaction?.size ?: "",
            brandLogo = this.data?.brand?.logoUrl ?: "",
            brandName = this.data?.brand?.name ?: "",
            brandColor = this.data?.brand?.color ?: "",
        )
    }
}

fun CommerceSession.Data?.toBrandSession(): BrandSession? {
    return if (this == null) {
        null
    } else {
        val transaction = this.transactions?.firstOrNull {
            it?.status == "requested" ||
                    it?.status == "approved"
        }
        val sessionId = id
        val transactionId = transaction?.id ?: ""
        val date =
            transaction?.expiresAt ?: (Instant.now().toEpochMilli() / 1000)
        BrandSession(sessionId = sessionId, transactionId = transactionId, date = date)
    }
}

internal fun ExchangeRate.getCurrencySign(): String? {
    return this.unitOfAccount?.toCurrencySign()
}

internal fun ExchangeRate.getAssetAmount(amount: String): String {
    return getAssetAmountValue(amount).toString()
}

internal fun ExchangeRate.getAssetAmountValue(amount: String): BigDecimal {
    val amountD = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val priceD = price?.toBigDecimalOrNull() ?: BigDecimal(1)
    val res = amountD.divide(priceD, precision?:0, RoundingMode.DOWN).stripTrailingZeros()
    return res
}

fun List<Long?>.getMinimum(): Long {
    return this.minOfOrNull { it ?: 0L } ?: 0L
}

fun List<ExchangeRate>.getByAssetId(id: String): ExchangeRate? {
    return this.firstOrNull { it.asset == id }
}

fun List<Long?>.getExpireTimeMills(
    currentTimestamp: Long,
    plusMillis: Long = 0,
): Long {
    val minimumExpireTime = this.getMinimum()
    val rateTimestamp = minimumExpireTime * 1000
    val diff = rateTimestamp + plusMillis - currentTimestamp
    return diff
}

fun AvailableAsset.hasBalanceRestrictions(): Boolean {
    val total = this.balanceBundle?.total ?: BigDecimal.ZERO
    val available = this.balanceBundle?.available ?: BigDecimal.ZERO
    return available > BigDecimal.ZERO && total != available
}

fun AvailableAsset.getSpendableBalance(): BigDecimal {
    val total = this.balanceBundle?.total ?: BigDecimal.ZERO
    val available = this.balanceBundle?.available
    return if (available != null && available != total) {
        available
    } else total
}

internal fun Context.getAppName(): String {
    return try {
        val application = this.applicationContext
        val packageInfo = when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ->
                application.packageManager
                    .getPackageInfo(application.packageName, 0)

            else -> application.packageManager
                .getPackageInfo(
                    application.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
        }
        application.getString(packageInfo.applicationInfo.labelRes)
    } catch (e: Exception) {
        "Inaccessible"
    }
}

internal fun Context.openEmail() {
    getActivity()?.let { activity ->
        try {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addCategory(Intent.CATEGORY_APP_EMAIL)
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e(null, "openEmail", e)
        }
    }
}

@Suppress(names = ["ModifierFactoryUnreferencedReceiver"])
internal fun Modifier.dragToReorder(
    item: BrandListItem,
    items: List<BrandListItem>,
    itemHeight: Int,
    updateSlideState: (item: BrandListItem, slideState: SlideState) -> Unit,
    isDraggable: Boolean,
    onStartDrag: () -> Unit,
    onStopDrag: (currentIndex: Int, destinationIndex: Int) -> Unit,
): Modifier = composed {
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    pointerInput(Unit) {
        coroutineScope {
            val currentIndex = items.indexOf(item)
            val offsetToSlide = itemHeight / 4
            var numberOfItems = 0
            var previousNumberOfItems: Int
            var listOffset = 0

            val onDragStart = {
                launch {
                    offsetX.stop()
                    offsetY.stop()
                }
                onStartDrag()
            }
            val onDrag = { change: PointerInputChange ->
                val horizontalDragOffset = offsetX.value + change.positionChange().x
                launch {
                    offsetX.snapTo(horizontalDragOffset)
                }
                val verticalDragOffset = offsetY.value + change.positionChange().y
                launch {
                    offsetY.snapTo(verticalDragOffset)
                    val offsetSign = offsetY.value.sign.toInt()
                    previousNumberOfItems = numberOfItems
                    numberOfItems = calculateNumberOfSlidItems(
                        offsetY.value * offsetSign,
                        itemHeight,
                        offsetToSlide,
                        previousNumberOfItems
                    )

                    if (previousNumberOfItems > numberOfItems) {
                        updateSlideState(
                            items[currentIndex + previousNumberOfItems * offsetSign],
                            SlideState.NONE
                        )
                    } else if (numberOfItems != 0) {
                        try {
                            updateSlideState(
                                items[currentIndex + numberOfItems * offsetSign],
                                if (offsetSign == 1) SlideState.UP else SlideState.DOWN
                            )
                        } catch (e: IndexOutOfBoundsException) {
                            numberOfItems = previousNumberOfItems
                            Log.i("DragToReorder", "Item is outside or at the edge")
                        }
                    }
                    listOffset = numberOfItems * offsetSign
                }
                if (change.positionChange() != Offset.Zero) change.consume()
            }
            val onDragEnd = {
                launch {
                    offsetX.animateTo(0f)
                }
                launch {
                    offsetY.animateTo(itemHeight * numberOfItems * offsetY.value.sign)
                    onStopDrag(currentIndex, currentIndex + listOffset)
                }
            }
            if (isDraggable)
                detectDragGestures(
                    onDragStart = {
                        onDragStart()
                    },
                    onDrag = { change, _ ->
                        onDrag(change)
                    },
                    onDragEnd = {
                        onDragEnd()
                    }
                ) else
                while (true) {
                    val pointerId = awaitPointerEventScope { awaitFirstDown().id }
                    awaitPointerEventScope {
                        drag(pointerId) { change ->
                            onDragStart()
                            onDrag(change)
                        }
                    }
                    onDragEnd()
                }
        }
    }
        .offset {
            IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt())
        }
}

private fun calculateNumberOfSlidItems(
    offsetY: Float,
    itemHeight: Int,
    offsetToSlide: Int,
    previousNumberOfItems: Int
): Int {
    val numberOfItemsInOffset = (offsetY / itemHeight).toInt()
    val numberOfItemsPlusOffset = ((offsetY + offsetToSlide) / itemHeight).toInt()
    val numberOfItemsMinusOffset = ((offsetY - offsetToSlide - 1) / itemHeight).toInt()
    return when {
        offsetY - offsetToSlide - 1 < 0 -> 0
        numberOfItemsPlusOffset > numberOfItemsInOffset -> numberOfItemsPlusOffset
        numberOfItemsMinusOffset < numberOfItemsInOffset -> numberOfItemsInOffset
        else -> previousNumberOfItems
    }
}
