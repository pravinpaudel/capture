package com.example.capture

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ImageView
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Manages CameraX lifecycle, image capture, and zoom functionality.
 * Encapsulates all camera-related operations to keep MainActivity clean.
 */
class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraControl: CameraControl? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    private var currentZoomRatio = 1f
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    /**
     * Initialize and start the camera with preview.
     * @param previewView The PreviewView to display camera feed
     * @param focusIndicator The ImageView to show focus animation
     * @param onError Callback when camera initialization fails
     */
    fun startCamera(
        previewView: PreviewView,
        focusIndicator: ImageView,
        onError: (Exception) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Build preview use case
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                // Build image capture use case
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()

                // Select back camera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                // Get camera control for zoom
                cameraControl = camera?.cameraControl

                // Setup tap-to-focus with indicator
                setupTapToFocus(previewView, focusIndicator)

            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed", e)
                onError(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Setup pinch-to-zoom gesture handling.
     * @param previewView The PreviewView to attach gesture detector
     */
    fun setupZoom(previewView: PreviewView) {
        scaleGestureDetector = ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    camera?.let {
                        val scale = currentZoomRatio * detector.scaleFactor
                        val cameraInfo = it.cameraInfo
                        val zoomState = cameraInfo.zoomState.value

                        if (zoomState != null) {
                            val clampedScale = scale.coerceIn(
                                zoomState.minZoomRatio,
                                zoomState.maxZoomRatio
                            )
                            cameraControl?.setZoomRatio(clampedScale)
                            currentZoomRatio = clampedScale
                        }
                    }
                    return true
                }
            })

        previewView.setOnTouchListener { view, event ->
            scaleGestureDetector.onTouchEvent(event)
            // Return false to allow tap-to-focus to work
            false
        }
    }

    /**
     * Setup tap-to-focus on the preview view with visual indicator.
     */
    private fun setupTapToFocus(previewView: PreviewView, focusIndicator: ImageView) {
        previewView.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_DOWN && !scaleGestureDetector.isInProgress) {
                // Get tap coordinates
                val x = event.x
                val y = event.y
                
                // Position focus indicator at tap location
                focusIndicator.x = x - focusIndicator.width / 2
                focusIndicator.y = y - focusIndicator.height / 2
                
                // Show focus indicator with animation
                showFocusIndicator(focusIndicator)
                
                // Trigger camera focus
                val factory = previewView.meteringPointFactory
                val point = factory.createPoint(x, y)
                val action = FocusMeteringAction.Builder(point).build()
                cameraControl?.startFocusAndMetering(action)
                
                return@setOnTouchListener true
            }
            scaleGestureDetector.onTouchEvent(event)
        }
    }
    
    /**
     * Show focus indicator with fade animation
     */
    private fun showFocusIndicator(focusIndicator: ImageView) {
        focusIndicator.visibility = View.VISIBLE
        focusIndicator.alpha = 0f
        focusIndicator.scaleX = 1.5f
        focusIndicator.scaleY = 1.5f
        
        // Fade in and scale down
        focusIndicator.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(200)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Hold for a moment, then fade out
                    focusIndicator.postDelayed({
                        focusIndicator.animate()
                            .alpha(0f)
                            .setDuration(300)
                            .setListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    focusIndicator.visibility = View.GONE
                                }
                            })
                            .start()
                    }, 800)
                }
            })
            .start()
    }

    /**
     * Capture a photo and save it to a file.
     * @param onSuccess Callback with the captured image URI
     * @param onError Callback when capture fails
     */
    fun capturePhoto(
        onSuccess: (Uri) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val imageCapture = imageCapture ?: run {
            onError(IllegalStateException("Camera not initialized"))
            return
        }

        val photoFile = createImageFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val uri = Uri.fromFile(photoFile)
                    onSuccess(uri)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    onError(exception)
                }
            }
        )
    }

    /**
     * Create a temporary file for storing captured images.
     */
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    /**
     * Release camera resources.
     */
    fun shutdown() {
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraManager"
    }
}
