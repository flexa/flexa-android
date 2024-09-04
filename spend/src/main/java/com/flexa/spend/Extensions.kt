package com.flexa.spend

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flexa.core.data.db.TransactionBundle
import com.flexa.core.entity.AppAccount
import com.flexa.core.entity.AssetKey
import com.flexa.core.entity.AvailableAsset
import com.flexa.core.entity.CommerceSession
import com.flexa.core.shared.SelectedAsset
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
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlin.math.sign


@Suppress("DEPRECATION")
internal fun Bitmap.blur(context: Context, radius: Float): Bitmap {
    // Create a new bitmap to hold the blurred result
    val blurredBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    // Create a RenderScript context
    val rs = RenderScript.create(context)

    // Create a script to perform the blur effect
    val script = ScriptIntrinsicBlur.create(rs, android.renderscript.Element.U8_4(rs))

    // Convert the input bitmap to a RenderScript allocation
    val input = Allocation.createFromBitmap(rs, this)

    // Convert the output bitmap to a RenderScript allocation
    val output = Allocation.createFromBitmap(rs, blurredBitmap)

    // Set the blur radius
    script.setRadius(radius)

    // Apply the blur effect
    script.setInput(input)
    script.forEach(output)

    // Copy the result back into the output bitmap
    output.copyTo(blurredBitmap)

    // Release the RenderScript resources
    rs.destroy()

    return blurredBitmap
}

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

internal fun String?.getAmount(): Double {
    val regex = """\d+(\.\d+)?""".toRegex()
    return regex.find(this?:"0")?.value?.toDoubleOrNull()?:0.0
}

internal fun AvailableAsset.logo(): String? =
    if (this.icon?.isNotBlank() == true)
        this.icon else this.assetData?.iconUrl

internal fun CommerceSession.label(): String? =
    this.data?.debits?.firstOrNull { it?.label != null }?.label

internal fun CommerceSession.transaction(): CommerceSession.Data.Transaction? =
    this.data?.transactions?.firstOrNull {
        it?.status == "requested"
    }

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

@Composable
fun rememberSelectedAsset(): State<SelectedAsset?> {
    val previewMode = LocalInspectionMode.current
    return if (!previewMode) Spend.selectedAsset.collectAsStateWithLifecycle()
    else remember { mutableStateOf(MockFactory.getMockSelectedAsset()) }
}

fun CommerceSession?.isValid(): Boolean {
    return this?.data != null && !this.data?.transactions.isNullOrEmpty() &&
            this.data?.status != "closed" &&
            this.data?.transactions?.any { it?.status == "requested" } == true
}

fun CommerceSession?.isNexGen() =
    if (this == null) {
        false
    } else {
        this.data?.brand?.legacyFlexcodes.isNullOrEmpty()
    }

fun CommerceSession?.isLegacy() = !this.isNexGen()

fun CommerceSession?.isCompleted() =
    this?.type == "commerce_session.completed"

fun CommerceSession?.containsAuthorization() =
    if (this == null) {
        false
    } else {
        this.data?.authorization != null &&
                this.data?.authorization?.number != null
    }

fun CommerceSession?.getAmount(): Double {
    return this?.data?.amount?.toDoubleOrNull() ?: 0.0
}

fun CommerceSession?.getAmountLabel(): String {
    return this?.data?.debits?.firstOrNull { !it?.amount.isNullOrEmpty() }?.label ?: ""
}

fun CommerceSession?.toTransaction(): Transaction? {
    return if (this == null) {
        null
    } else {
        val currentTransaction = this.data?.transactions?.firstOrNull {
            it?.status == "requested"
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
fun CommerceSession?.toTransactionBundle(): TransactionBundle? {
    return if (this == null) {
        null
    } else {
        val transaction = this.data?.transactions?.firstOrNull {
            it?.status == "requested"
        }
        val sessionId = data?.id?:""
        val transactionId = transaction?.id?:""
        val date =
            transaction?.expiresAt ?: (Instant.now().toEpochMilli() / 1000)
        TransactionBundle(
            transactionId, sessionId, date
        )
    }
}

internal fun Context.getAppName(): String {
      return  try {
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
                // Consume the gesture event, not passed to external
                change.consumePositionChange()
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
            // Use the animating offset value here.
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
