package com.example.capture

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * Main Activity for camera-based event capture.
 * Follows MVVM architecture with clean separation of concerns.
 * Responsibilities: UI logic, view observation, user interaction handling.
 */
class MainActivity : AppCompatActivity() {

    // ViewModels and Helpers
    private val viewModel: CameraViewModel by viewModels {
        CameraViewModelFactory(OcrProcessor(this))
    }
    private val permissionHandler by lazy { PermissionHandler(this) }
    private val cameraManager by lazy { CameraManager(this, this) }

    // UI Components
    private lateinit var cameraPreview: PreviewView
    private lateinit var captureButton: FloatingActionButton
    private lateinit var galleryButton: FloatingActionButton
    private lateinit var focusIndicator: ImageView
    private var progressDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupFullscreen()
        setContentView(R.layout.activity_main)

        initializeViews()
        setupObservers()
        setupClickListeners()
        requestCameraPermissionAndStart()
    }

    /**
     * Initialize all view references.
     */
    private fun initializeViews() {
        cameraPreview = findViewById(R.id.camera_preview)
        captureButton = findViewById(R.id.capture_button)
        galleryButton = findViewById(R.id.gallery_button)
        focusIndicator = findViewById(R.id.focus_indicator)
    }

    /**
     * Setup observers for ViewModel LiveData.
     */
    private fun setupObservers() {
        // Observe processing state
        viewModel.isProcessing.observe(this) { isProcessing ->
            if (isProcessing) {
                showProgressDialog()
            } else {
                dismissProgressDialog()
            }
        }

        // Observe OCR result
        viewModel.ocrResult.observe(this) { result ->
            result.onSuccess { eventData ->
                val imageUri = viewModel.imageUri.value
                navigateToEventForm(imageUri, eventData)
            }.onFailure { error ->
                showError("Failed to process image: ${error.message}")
            }
        }
    }

    /**
     * Setup button click listeners.
     */
    private fun setupClickListeners() {
        captureButton.setOnClickListener {
            capturePhoto()
        }

        galleryButton.setOnClickListener {
            pickFromGallery()
        }
    }

    /**
     * Request camera permission and start camera.
     */
    private fun requestCameraPermissionAndStart() {
        permissionHandler.checkCameraPermission(
            onGranted = { startCamera() },
            onDenied = { message ->
                showError(message)
                finish()
            }
        )
    }

    /**
     * Start the camera with preview and zoom.
     */
    private fun startCamera() {
        cameraManager.startCamera(
            previewView = cameraPreview,
            focusIndicator = focusIndicator,
            onError = { error ->
                showError("Failed to start camera: ${error.message}")
            }
        )
        cameraManager.setupZoom(cameraPreview)
    }

    /**
     * Capture a photo using camera.
     */
    private fun capturePhoto() {
        cameraManager.capturePhoto(
            onSuccess = { uri ->
                viewModel.setImageUri(uri)
                launchCropActivity(uri)
            },
            onError = { error ->
                showError("Failed to capture photo: ${error.message}")
            }
        )
    }

    /**
     * Pick an image from gallery.
     */
    private fun pickFromGallery() {
        permissionHandler.checkStoragePermission(
            onGranted = { pickImageLauncher.launch("image/*") },
            onDenied = { message -> showError(message) }
        )
    }

    /**
     * Gallery image picker result handler.
     */
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setImageUri(uri)
            launchCropActivity(uri)
        } else {
            showError("No image selected")
        }
    }

    /**
     * Launch crop activity with image.
     */
    private fun launchCropActivity(imageUri: Uri?) {
        val intent = Intent(this, CropActivity::class.java)
            .apply { putExtra("photo_uri", imageUri) }
        cropImageLauncher.launch(intent)
    }

    /**
     * Crop activity result handler.
     */
    private val cropImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val croppedImageUri = result.data?.getParcelableExtra<Uri>("cropped_image_uri")
            if (croppedImageUri != null) {
                viewModel.processImageWithOCR(croppedImageUri)
            } else {
                showError("Failed to get cropped image")
            }
        }
    }

    /**
     * Navigate to event form with extracted data.
     */
    private fun navigateToEventForm(photoUri: Uri?, eventData: RawEventData) {
        val intent = Intent(this, EventFormActivity::class.java).apply {
            putExtra("photo_uri", photoUri)
            putExtra("event_title", eventData.title)
            putExtra("event_date", eventData.date)
            putExtra("event_time", eventData.time)
            putExtra("event_location", eventData.location)
            putExtra("event_description", eventData.description)
        }
        startActivity(intent)
    }

    /**
     * Enable fullscreen immersive mode.
     */
    private fun setupFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    /**
     * Show progress dialog while processing.
     */
    private fun showProgressDialog() {
        progressDialog = AlertDialog.Builder(this)
            .setTitle("Processing image")
            .setMessage("Please wait...")
            .setCancelable(false)
            .create()
        progressDialog?.show()
    }

    /**
     * Dismiss progress dialog.
     */
    private fun dismissProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    /**
     * Show error message to user.
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        progressDialog?.dismiss()
        cameraManager.shutdown()
    }
}
