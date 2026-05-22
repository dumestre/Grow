package com.daime.grow.ui.screen.ppfd

import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.log10
import kotlin.math.pow

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

    AndroidView(
        factory = { previewView },
        modifier = modifier,
        update = {
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val lux = calculateLuxFromImage(imageProxy)
                    onLuxUpdate(lux)
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
 * Calcula uma estimativa de LUX baseada na luminosidade média da imagem.
 * Nota: Isso é uma aproximação e pode variar entre dispositivos.
 * Idealmente usaríamos metadados de exposição (ISO, tempo), mas o CameraX
 * abstrai isso e nem sempre é acessível em tempo real no analyzer de forma simples.
 */
private fun calculateLuxFromImage(image: ImageProxy): Float {
    val plane = image.planes[0] // Plano Y em YUV_420_888
    val buffer = plane.buffer
    val data = ByteArray(buffer.remaining())
    buffer.get(data)
    
    var sum = 0L
    for (i in data.indices step 10) { // Amostragem para performance
        sum += data[i].toInt() and 0xFF
    }
    
    val avgLuminance = sum.toFloat() / (data.size / 10f)
    
    // Mapeamento logarítmico aproximado de 0-255 para LUX (0-100000)
    // Lux = 10 ^ (Luminance / 50) - calibração empírica
    val baseLux = 10.0.pow(avgLuminance.toDouble() / 60.0).toFloat()
    
    // Multiplicador de escala para alinhar com sensores comuns
    return (baseLux * 2f).coerceIn(0f, 150000f)
}
