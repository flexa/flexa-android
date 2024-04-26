package com.flexa.scan

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.YuvImage
import android.media.Image
import android.util.Log
import android.util.Rational
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@OptIn(ExperimentalPermissionsApi::class)
@androidx.compose.ui.tooling.preview.Preview
@Composable
fun FlexaScanner(
    modifier: Modifier = Modifier,
    pointsOfInterest: RectF? = null,
    onQrCode: (List<String>) -> Unit = {},
    onBitmap: (Bitmap) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )


    DisposableEffect(Unit) {
        onDispose { cameraProviderFuture.get().unbindAll() }
    }

    when (cameraPermissionState.status) {
        PermissionStatus.Granted -> {
            AndroidView(
                modifier = modifier,
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                },
                update = { p ->
                    val cameraSelector: CameraSelector = CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()
                    val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
                    cameraProviderFuture.addListener({
                        val preview = Preview.Builder()
                            .build()
                            .also {
                                it.setSurfaceProvider(p.surfaceProvider)
                            }

                        val width = p.width
                        val height = p.height

                        val cameraProvider = cameraProviderFuture.get()
                        val analyzer = QRCodeImageAnalyzer(
                            pointsOfInterest,
                            Size(width, height),
                            { qrCodeData ->
                                onQrCode.invoke(qrCodeData)
                                if (qrCodeData.isNotEmpty())
                                    Scan.qrCodesCallback?.invoke(Result.success(qrCodeData))
                            }, {
                                onBitmap.invoke(it)
                            }
                        )

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setTargetResolution(Size(width, height))
                            .build()
                            .apply { setAnalyzer(cameraExecutor, analyzer) }

                        val viewPort =
                            ViewPort.Builder(Rational(width, height), preview.targetRotation)
                                .setScaleType(ViewPort.FIT)
                                .build()
                        val useCaseGroup = UseCaseGroup.Builder()
                            .addUseCase(preview)
                            .addUseCase(imageAnalysis)
                            .setViewPort(viewPort)
                            .build()


                        try {
                            cameraProvider?.unbindAll()
                            cameraProvider?.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                useCaseGroup
                            )
                        } catch (e: Exception) {
                            Log.e("", e.message, e)
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            )

        }
        is PermissionStatus.Denied -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .zIndex(1f)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val textToShow =
                        if ((cameraPermissionState.status as PermissionStatus.Denied).shouldShowRationale) {
                            // If the user has denied the permission but the rationale can be shown,
                            // then gently explain why the app requires this permission
                            "The camera is important for the Scanner. Please grant the permission."
                        } else {
                            // If it's the first time the user lands on this feature, or the user
                            // doesn't want to be asked again for this permission, explain that the
                            // permission is required
                            "Camera permission required for this feature to be available. " +
                                    "Please grant the permission"
                        }
                    Text(
                        modifier = Modifier.width(200.dp),
                        text = textToShow,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                        Text("Request permission")
                    }
                }
            }
        }
    }
}

private class QRCodeImageAnalyzer(
    val pointsOfInterest: RectF?,
    val previewSize: Size,
    val qrCodeCallback: (List<String>) -> Unit,
    val onBitmap: (Bitmap) -> Unit
) : ImageAnalysis.Analyzer {

    val showPoiImage = false
    val scope = CoroutineScope(Dispatchers.Main)
    var resizedBmp: Bitmap? = null

    private val options by lazy(LazyThreadSafetyMode.NONE) {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
    }
    private val scanner by lazy(LazyThreadSafetyMode.NONE) {
        BarcodeScanning.getClient(options)
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
//        imageProxy.setCropRect(Rect(154, 819, 926, 1591))
//        pointsOfInterest?.set(RectF(154f, 784f, 926f, 1556f))
        val mediaImage = imageProxy.image
        if (mediaImage != null) {

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

            val x = pointsOfInterest?.left?.coerceAtLeast(1f) ?: 1f//154
            val y = pointsOfInterest?.top?.coerceAtLeast(1f) ?: 1f//819


            if (pointsOfInterest != null) {
                val width =
                    ((pointsOfInterest?.right ?: 1f) - (pointsOfInterest?.left ?: 0f))
                        .coerceAtLeast(1f)//(926 - 154)
                val height =
                    ((pointsOfInterest?.bottom ?: 1f) - (pointsOfInterest?.top ?: 0f))
                        .coerceAtLeast(1f)//(1591 - 819)
                resizedBmp = Bitmap.createBitmap(
                    scaledBitmap,
                    x.toInt(),
                    y.toInt(),
                    width.toInt(),
                    height.toInt()
                )
            } else {
                resizedBmp = scaledBitmap.copy(Bitmap.Config.ARGB_8888, false)
            }
            scaledBitmap.recycle()

            resizedBmp?.let { bmp ->
                if (showPoiImage) onBitmap.invoke(bmp)
                scanner.process(bmp, 0)
                    .addOnSuccessListener { barcodes ->
//                        Log.d(
//                            "QRCodeImageAnalyzer",
//                            "analyze: >>> >${barcodes.map { it.rawValue }}<"
//                        )
                        barcodes.let { codes ->
                            qrCodeCallback.invoke(
                                codes.filter { it.rawValue != null }
                                    .map { it.rawValue!! })
                        }
                    }
                    .addOnFailureListener {
                        Log.e(javaClass.simpleName, it.message, it)
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

fun Image.toBitmap(): Bitmap {
    val yBuffer = planes[0].buffer // Y
    val vuBuffer = planes[2].buffer // VU

    val ySize = yBuffer.remaining()
    val vuSize = vuBuffer.remaining()

    val nv21 = ByteArray(ySize + vuSize)

    yBuffer.get(nv21, 0, ySize)
    vuBuffer.get(nv21, ySize, vuSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}
