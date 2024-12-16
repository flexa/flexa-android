package com.flexa.scan

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Rational
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FlexaScanner(
    modifier: Modifier = Modifier,
    pointsOfInterest: RectF,
    flashLight: Boolean = false,
    onQrCode: (List<String>) -> Unit = {},
    onBitmap: (Bitmap) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when(event) {
                Lifecycle.Event.ON_PAUSE -> {
                    cameraProviderFuture.get().unbindAll()
                }
                else -> { }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    when (cameraPermissionState.status) {
        PermissionStatus.Granted -> {
            AndroidView(
                modifier = modifier,
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    }
                },
                update = { view ->
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
                    cameraProviderFuture.addListener({
                        val width = view.width
                        val height = view.height
                        val resolutionSelector = ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                ResolutionStrategy(
                                    Size(width, height),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER
                                )
                            ).build()

                        val preview = Preview.Builder()
                            .setResolutionSelector(resolutionSelector)
                            .build()
                            .also { it.setSurfaceProvider(view.surfaceProvider)  }

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

                        runCatching {
                            cameraProvider?.unbindAll()
                            val camera = cameraProvider?.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                useCaseGroup
                            )
                            if (camera?.cameraInfo?.hasFlashUnit() == true) {
                                camera.cameraControl.enableTorch(flashLight)
                            }
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
                            stringResource(R.string.permission_copy)
                        } else {
                            stringResource(R.string.permission_copy_please)
                        }
                    Text(
                        modifier = Modifier.width(200.dp),
                        text = textToShow,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                        Text("Request permission")
                    }
                }
            }
        }
    }
}
