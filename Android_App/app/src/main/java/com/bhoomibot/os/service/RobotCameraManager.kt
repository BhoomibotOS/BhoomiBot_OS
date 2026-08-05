package com.bhoomibot.os.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.bhoomibot.os.connection.model.VideoQuality
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max

/**
 * FEATURE: High-Fidelity Vision Bridge
 * 
 * AI-Fix: Uses high-resolution sensor capture and high-bitrate encoding to ensure 
 * the operator sees exactly what the robot sees.
 */
object RobotCameraManager {

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var preview: Preview? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var lastAnalyzeTs = 0L

    var isBroadcasting = false
        private set
    
    private var targetLongestSide = 1280
    private var targetJpegQuality = 80

    fun startCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        quality: VideoQuality = VideoQuality.MEDIUM,
        useRearCamera: Boolean = true,
        onFrame: (ByteArray) -> Unit,
        onBitmap: (Bitmap) -> Unit
    ) {
        this.targetLongestSide = quality.longestSide
        this.targetJpegQuality = quality.jpegQuality
        
        Log.i("RobotCamera", "Starting camera with target: ${quality.label} (Side: $targetLongestSide, Qual: $targetJpegQuality, Rear: $useRearCamera)")

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // AI-Fix: Professional Resolution Selector
            // We request the hardware to capture at 1080p (FHD) always.
            // This is the source of quality. We only downscale AFTER capture if needed.
            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(1920, 1080), 
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER
                    )
                )
                .build()

            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(resolutionSelector)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { proxy ->
                        processImageProxy(proxy, onFrame, onBitmap)
                    }
                }

            preview = Preview.Builder()
                .setResolutionSelector(resolutionSelector)
                .build()

            val cameraSelector = if (useRearCamera) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalyzer)
                isBroadcasting = true
            } catch (e: Exception) {
                Log.e("RobotCamera", "Binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun setPreviewView(previewView: PreviewView?) {
        preview?.setSurfaceProvider(previewView?.surfaceProvider)
    }

    fun stopCamera() {
        cameraProvider?.unbindAll()
        isBroadcasting = false
        preview = null
        imageAnalyzer = null
    }

    private fun processImageProxy(
        proxy: ImageProxy,
        onFrame: (ByteArray) -> Unit,
        onBitmap: (Bitmap) -> Unit
    ) {
        val now = System.currentTimeMillis()
        val interval = 1000L / 12 
        if (now - lastAnalyzeTs < interval) {
            proxy.close()
            return
        }
        lastAnalyzeTs = now

        val raw = runCatching { proxy.toBitmap() }.getOrNull() ?: run {
            proxy.close()
            return
        }
        
        val rotation = proxy.imageInfo.rotationDegrees
        proxy.close()

        val bitmap = if (rotation != 0) {
            val m = Matrix().apply { postRotate(rotation.toFloat()) }
            val rotated = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
            raw.recycle()
            rotated
        } else raw

        // Pass 1080p bitmap to AI
        onBitmap(bitmap)

        // Pass high-quality JPEG to Operator
        val resized = resizeToLongestSide(bitmap, targetLongestSide)
        val stream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, targetJpegQuality, stream)
        onFrame(stream.toByteArray())
        
        if (resized != bitmap) resized.recycle()
        bitmap.recycle()
    }

    private fun resizeToLongestSide(bmp: Bitmap, longest: Int): Bitmap {
        val w = bmp.width
        val h = bmp.height
        val currentLongest = max(w, h)
        if (currentLongest <= longest) return bmp
        
        val scale = longest.toFloat() / currentLongest
        return Bitmap.createScaledBitmap(bmp, max(1, (w * scale).toInt()), max(1, (h * scale).toInt()), true)
    }
}
