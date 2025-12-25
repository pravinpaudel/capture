package com.example.capture

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var ocrProcessor: OcrProcessor
    private var progressDialog: AlertDialog? = null
    private var photoFile: File? = null
    private var photoUri: Uri? = null
    private lateinit var photoView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ocrProcessor = OcrProcessor(this)
        photoView = findViewById(R.id.image_preview)

        // Show dialog to choose between camera and gallery
        showImageSourceDialog()
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(this)
            .setTitle("Select Image Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndCapture()
                    1 -> checkStoragePermissionAndPick()
                }
            }
            .setOnCancelListener {
                finish() // Close app if user cancels
            }
            .show()
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

    private val requestStoragePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            pickFromGallery()
        } else {
            Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun pickFromGallery() {
        pickImageLauncher.launch("image/*")
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            photoUri = uri
            onPhotoCaptured()
        } else {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun checkCameraPermissionAndCapture() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED ->
                startCameraCapture()
            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startCameraCapture()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    private fun startCameraCapture() {
        val file = createImageFile() // Create an empty .jpg file
        photoFile = file
        // Convert the file to a URI for the camera intent. (safe, permission‑controlled way to give the camera app access to your file.)
        photoUri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", file)
        takePictureLauncher.launch(photoUri)
    }
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_" // _ at the end because createTempFile will add random number at the end
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: filesDir // fallback
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    // Create a launcher for the camera
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoFile != null) {
            onPhotoCaptured()
        } else {
            Toast.makeText(this, "Failed to capture photo", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun onPhotoCaptured() {
        showProgressDialog()
        photoView.setImageURI(photoUri)

        lifecycleScope.launch {
            try {
                val eventDetails = ocrProcessor.extractAndParseEvent(photoUri!!)

                dismissProgressDialog()

                if (eventDetails.isSuccess) {
                    val eventData = eventDetails.getOrThrow()
                    navigateToEventForm(photoUri, eventData)
                } else {
                    val errorMessage = eventDetails.exceptionOrNull()?.message ?: "Unknown error"
                    Toast.makeText(this@MainActivity, "Error: $errorMessage", Toast.LENGTH_LONG)
                        .show()
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
    }

}

