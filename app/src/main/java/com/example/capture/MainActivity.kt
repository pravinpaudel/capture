package com.example.capture

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.ScaleGestureDetector
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var ocrProcessor: OcrProcessor
    private var progressDialog: AlertDialog? = null
    private var photoFile: File? = null
    private var photoUri: Uri? = null
    
    // CameraX components
    private lateinit var cameraPreview: PreviewView
    private lateinit var captureButton: FloatingActionButton
    private lateinit var galleryButton: FloatingActionButton
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraControl: CameraControl? = null
    private lateinit var cameraExecutor: ExecutorService
    
    // Zoom functionality
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var currentZoomRatio = 1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable fullscreen immersive mode
        setupFullscreen()
        
        setContentView(R.layout.activity_main)

        ocrProcessor = OcrProcessor(this)
        
        // Initialize views
        cameraPreview = findViewById(R.id.camera_preview)
        captureButton = findViewById(R.id.capture_button)
        galleryButton = findViewById(R.id.gallery_button)
        
        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor() // Create a new background thread for camera operations
        
        // Setup pinch-to-zoom
        setupPinchToZoom()
        
        // Set button click listeners
        captureButton.setOnClickListener {
            capturePhoto()
        }
        
        galleryButton.setOnClickListener {
            checkStoragePermissionAndPick()
        }
        
        // Request camera permission and start camera
        checkCameraPermissionAndStartCamera()
    }
    
    private fun setupFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false) // Let my layout extend behind the system bars, for fullscreen
        val controller = WindowInsetsControllerCompat(window, window.decorView) // This creates a controller object that lets you **show or hide** system bars.
        controller.hide(WindowInsetsCompat.Type.systemBars()) // Hide the system bars
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE // Allow the user to swipe the bars to show/hide them
    }
    
    private fun setupPinchToZoom() {
        // ScaleGestureDetector is an Android class that detects pinch gestures.
        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                camera?.let { // Check if camera exists
                    val scale = currentZoomRatio * detector.scaleFactor // detector.scaleFactor - how much is user pinching - 1.0 -zoom in, <1.0 zoom out
                    val cameraInfo = it.cameraInfo
                    val zoomState = cameraInfo.zoomState.value // Get zoom state - minZoomRatio, maxZoomRatio, currentZoomRatio
                    
                    if (zoomState != null) {
                        // Clamp the scale so it doesn't exceed the zoom limits
                        val clampedScale = scale.coerceIn(
                            zoomState.minZoomRatio,
                            zoomState.maxZoomRatio
                        )
                        // Apply the zoom to the camera
                        cameraControl?.setZoomRatio(clampedScale)
                        currentZoomRatio = clampedScale
                    }
                }
                return true // Yes, I handled this gesture.
            }
        })

        // Attach the gesture detector to the PreviewView
        cameraPreview.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            true
        }
    }
    
    private fun checkCameraPermissionAndStartCamera() {
        when { // if granted, start camera, else request permission
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA // Permission we're checking
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required to use this app", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun startCamera() {
        // ProcessCameraProvider controls all camera operations
        // does NOT return the camera provider immediately. It returns a future:
        // Start preparing the camera provider in the background. When it’s ready, notify me.
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        // Add a listener to the camera provider future.
        cameraProviderFuture.addListener({
            try {
                // The real camera provider that can be used to bind the use cases
                val cameraProvider = cameraProviderFuture.get()
                
                // Preview. Creating a use case object for the camera. It is not active yet (not consuming any frames, not using any hardware)
                val preview = Preview.Builder()
                    .build() // Create a preview instance but it's not connected to the camera yet, no surface to display on.
                    .also {
                        it.setSurfaceProvider(cameraPreview.surfaceProvider) // Connect the preview use case to the preview view. Surface Provider - where to send the camera frames
                    }
                
                // ImageCapture with optimized settings
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY) // best quality, slower capture
                    .build()
                
                // Select back camera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()
                
                // Bind use cases to camera and store camera reference
                camera = cameraProvider.bindToLifecycle( // This is when the camera opens and binds the actual use case
                    this, cameraSelector, preview, imageCapture
                )
                
                // Get camera control for zoom
                cameraControl = camera?.cameraControl
                
                // Enable tap to focus
                cameraPreview.setOnClickListener { view ->
                    val factory = cameraPreview.meteringPointFactory
                    val point = factory.createPoint(view.width / 2f, view.height / 2f)
                    val action = androidx.camera.core.FocusMeteringAction.Builder(point).build()
                    cameraControl?.startFocusAndMetering(action)
                }
                
            } catch (e: Exception) {
                Log.e("MainActivity", "Camera initialization failed", e)
                Toast.makeText(this, "Failed to start camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun capturePhoto() {
        val imageCapture = imageCapture ?: return
        
        // Create file for the captured image
        val photoFile = createImageFile()
        this.photoFile = photoFile

        // Saved the image to the file
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        
        imageCapture.takePicture( // Triggers the actual photo capture
            outputOptions,
            ContextCompat.getMainExecutor(this), // Runs the callback on the main thread (UI thread)
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    photoUri = Uri.fromFile(photoFile)
                    onPhotoCaptured()
                }
                
                override fun onError(exception: ImageCaptureException) {
                    Log.e("MainActivity", "Photo capture failed: ${exception.message}", exception)
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to capture photo: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun checkStoragePermissionAndPick() {
        // For Android 13+ (API 33+), we need READ_MEDIA_IMAGES
        // For older versions, we need READ_EXTERNAL_STORAGE
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                pickFromGallery()
            }
            else -> {
                requestStoragePermissionLauncher.launch(permission)
            }
        }
    }

    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pickFromGallery()
        } else {
            Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickFromGallery() {
        pickImageLauncher.launch("image/*")
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUri = uri
            onPhotoCaptured()
        } else {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: filesDir
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    private fun onPhotoCaptured() {
        showProgressDialog()

        // Run the task on a coroutine. `lifecycleScope` ensures the task is cancelled if the Activity is destroyed
        lifecycleScope.launch {
            try {
                val eventDetails = ocrProcessor.extractAndParseEvent(photoUri!!)

                dismissProgressDialog()

                if (eventDetails.isSuccess) {
                    val eventData = eventDetails.getOrThrow()
                    navigateToEventForm(photoUri, eventData)
                } else {
                    val errorMessage = eventDetails.exceptionOrNull()?.message ?: "Unknown error"
                    Toast.makeText(this@MainActivity, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                dismissProgressDialog()
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateToEventForm(photoUri: Uri?, eventData: EventData) {
        val intent = Intent(this, EventFormActivity::class.java)
        intent.putExtra("photo_uri", photoUri)
        intent.putExtra("event_title", eventData.title)
        intent.putExtra("event_date", eventData.date)
        intent.putExtra("event_time", eventData.time)
        intent.putExtra("event_location", eventData.location)
        intent.putExtra("event_description", eventData.description)
        startActivity(intent)
    }

    private fun showProgressDialog() {
        progressDialog = AlertDialog.Builder(this)
            .setTitle("Processing image")
            .setMessage("Please wait...")
            .setCancelable(false)
            .create()
        progressDialog?.show()
    }

    private fun dismissProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    override fun onDestroy() {
        super.onDestroy()
        ocrProcessor.close()
        progressDialog?.dismiss()
        cameraExecutor.shutdown()
    }

}

