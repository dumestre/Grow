package com.daime.grow.ui.screen.ppfd

import android.hardware.camera2.CaptureResult
import android.util.Log
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.pow

@ExperimentalCamera2Interop
@Composable
fun CameraLightMeter(
    onLuxUpdate: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    
    val previewView = remember { PreviewView(context) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // Filtro de média móvel para estabilidade
    val luxHistory = remember { mutableListOf<Float>() }
    val historySize = 10

    AndroidView(
        factory = { previewView },
        modifier = modifier,
        update = {
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val lux = calculateLuxWithCompensation(imageProxy)
                    
                    if (lux > 0) {
                        luxHistory.add(lux)
                        if (luxHistory.size > historySize) luxHistory.removeAt(0)
                        
                        val smoothedLux = luxHistory.average().toFloat()
                        onLuxUpdate(smoothedLux)
                    }
                    
                    imageProxy.close()
                }

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("CameraLightMeter", "Binding failed", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

/**
 * Calcula LUX compensando as variações de ISO e Tempo de Exposição da câmara (AE).
 */
@ExperimentalCamera2Interop
private fun calculateLuxWithCompensation(image: ImageProxy): Float {
    val result = image.imageInfo.cameraCaptureResult
    
    // Parâmetros de exposição em tempo real
    val exposureTimeNs = result?.get(CaptureResult.SENSOR_EXPOSURE_TIME) ?: 10_000_000L // default 1/100s
    val sensitivityIso = result?.get(CaptureResult.SENSOR_SENSITIVITY) ?: 100 // default ISO 100
    val aperture = result?.get(CaptureResult.LENS_APERTURE) ?: 2.0f // default f/2.0
    
    // Luminosidade média dos pixels (Plano Y)
    val plane = image.planes[0]
    val buffer = plane.buffer
    val data = ByteArray(buffer.remaining())
    buffer.get(data)
    
    var sum = 0L
    val step = 15 // Amostragem para performance
    for (i in data.indices step step) {
        sum += data[i].toInt() and 0xFF
    }
    val avgLuminance = sum.toFloat() / (data.size / step.toFloat())
    
    /**
     * Fórmula PAR/LUX baseada em exposição:
     * Lux = (L * N²) / (t * S) * C
     * L = Luminância média (0-255)
     * N = Abertura (Aperture)
     * t = Tempo de exposição em segundos
     * S = ISO do sensor
     * C = Constante de calibração
     */
    val exposureTimeSec = exposureTimeNs / 1_000_000_000.0
    val calibrationConstant = 50.0 // Valor empírico para ajuste fino
    
    val rawLux = (avgLuminance * aperture.toDouble().pow(2.0)) / 
                 (exposureTimeSec * sensitivityIso) * calibrationConstant
                 
    return rawLux.toFloat().coerceIn(0f, 150000f)
}

private val ImageInfo.cameraCaptureResult: CaptureResult?
    get() = runCatching {
        val result = Class
            .forName("androidx.camera.core.CameraCaptureResults")
            .getMethod("retrieveCameraCaptureResult", ImageInfo::class.java)
            .invoke(null, this)

        result?.javaClass
            ?.getMethod("getCaptureResult")
            ?.invoke(result) as? CaptureResult
    }.getOrNull()
