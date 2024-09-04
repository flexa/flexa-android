package com.flexa.scan

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class QRCodeImageAnalyzer(
    private val pointsOfInterest: RectF,
    private val previewSize: Size,
    private val qrCodeCallback: (List<String>) -> Unit,
    private val onBitmap: (Bitmap) -> Unit
) : ImageAnalysis.Analyzer {

    private val showPoiImage = false
    private val scope = CoroutineScope(Dispatchers.Main)
    private var resizedBmp: Bitmap? = null

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .enableAllPotentialBarcodes()
        .build()
    private val scanner = BarcodeScanning.getClient(options)


    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        imageProxy.image?.let { mediaImage ->
            val image = mediaImage.toBitmap()
            val rotatedBitmap = Bitmap.createBitmap(
                image, 0, 0, image.width, image.height,
                Matrix().apply { postRotate(imageProxy.imageInfo.rotationDegrees.toFloat()) }, true
            )
            image.recycle()
            val scaledBitmap = Bitmap.createScaledBitmap(
                rotatedBitmap,
                previewSize.width,
                previewSize.height,
                true
            )
            rotatedBitmap.recycle()

            val x = pointsOfInterest.left.coerceAtLeast(1f)
            val y = pointsOfInterest.top.coerceAtLeast(1f)


            val width =
                (pointsOfInterest.right - pointsOfInterest.left)
                    .coerceAtLeast(1f)
            val height =
                (pointsOfInterest.bottom - pointsOfInterest.top)
                    .coerceAtLeast(1f)
            resizedBmp = Bitmap.createBitmap(
                scaledBitmap,
                x.toInt(),
                y.toInt(),
                width.toInt(),
                height.toInt()
            )
            scaledBitmap.recycle()

            resizedBmp?.let { bmp ->
                if (showPoiImage) onBitmap.invoke(bmp)
                scanner.process(bmp, 0)
                    .addOnSuccessListener { barcodes ->
                        barcodes.let { codes ->
                            qrCodeCallback.invoke(
                                codes.filter { it.rawValue != null }
                                    .map { it.rawValue!! })
                        }
                    }
                    .addOnFailureListener {
                        Log.e(null, it.message, it)
                    }
                    .addOnCompleteListener {
                        scope.launch {
                            delay(300)
                            imageProxy.close()
                        }
                        if (!showPoiImage) bmp.recycle()
                    }
            }
        }
    }
}
