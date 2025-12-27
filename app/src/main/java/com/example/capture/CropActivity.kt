package com.example.capture

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.canhub.cropper.CropImageView
import com.google.android.material.button.MaterialButton
import kotlin.Suppress

/**
 * Professional crop activity with Google Lens-style UI.
 * Fullscreen immersive experience for precise image cropping.
 */
class CropActivity: AppCompatActivity() {
    private lateinit var cropImageView: CropImageView
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnCrop: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupFullscreen()
        setContentView(R.layout.activity_crop)

        // Initialize views
        cropImageView = findViewById(R.id.cropImageView)
        btnCancel = findViewById(R.id.btnCancel)
        btnCrop = findViewById(R.id.btnCrop)

        // Get photo URI from intent
        @Suppress("DEPRECATION")
        val photoUri: Uri? = intent.getParcelableExtra("photo_uri")

        if(photoUri == null){
            Toast.makeText(this, "Error: No photo URI provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Set image to crop
        cropImageView.setImageUriAsync(photoUri)

        setupListeners()
    }

    /**
     * Enable fullscreen immersive mode like MainActivity.
     */
    private fun setupFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun setupListeners() {
        btnCancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        btnCrop.setOnClickListener {
            // Handle crop action and call the callback function once the crop is completed
            cropImageView.croppedImageAsync()
        }

        // Callback function that's called when the crop is completed
        cropImageView.setOnCropImageCompleteListener { view, result ->
            if (result.isSuccessful) {
                val croppedUri = result.uriContent
                Log.d("CropActivity", "Crop successful: $croppedUri")

                // Return cropped image URI to MainActivity
                val resultIntent = Intent().apply {
                    putExtra("cropped_image_uri", croppedUri)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                val error = result.error
                Log.e("CropActivity", "Crop error", error)
                Toast.makeText(this, "Crop failed: ${error?.message}", Toast.LENGTH_LONG).show()
            }

        }
    }

}