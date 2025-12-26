package com.example.capture

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.canhub.cropper.CropImageView
import kotlin.Suppress

class CropActivity: AppCompatActivity() {
    private lateinit var cropImageView: CropImageView
    private lateinit var btnCancel: Button
    private lateinit var btnCrop: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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