package com.example.capture

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var photoFile: File? = null
    private var photoUri: Uri? = null
    private lateinit var photoView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        photoView = findViewById(R.id.image_preview)

        // Launch camera directly when app opens
        checkCameraPermissionAndCapture()
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
            // Handle permission denied
            // You can show a message to the user or disable the feature
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
        if (success) {
            photoView.setImageURI(photoUri)
            // Navigate to event form with the captured photo
            val intent = Intent(this, EventFormActivity::class.java)
            intent.putExtra("photo_uri", photoUri)
            startActivity(intent)
        } else {
            // Handle failure to capture photo
            // You can show a message to the user or disable the feature
        }
    }
}

