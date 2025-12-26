package com.example.capture

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Handles runtime permission requests for camera and storage access.
 * Simplifies permission management by encapsulating permission logic and Activity Result API.
 */
class PermissionHandler(private val activity: AppCompatActivity) {

    private var cameraPermissionCallback: (() -> Unit)? = null
    private var storagePermissionCallback: (() -> Unit)? = null
    private var permissionDeniedCallback: ((String) -> Unit)? = null

    private val cameraPermissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                cameraPermissionCallback?.invoke()
            } else {
                permissionDeniedCallback?.invoke("Camera permission is required to use this app")
            }
        }

    private val storagePermissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                storagePermissionCallback?.invoke()
            } else {
                permissionDeniedCallback?.invoke("Storage permission is required to select images")
            }
        }

    /**
     * Check camera permission and request if needed.
     * @param onGranted Callback when permission is granted
     * @param onDenied Optional callback when permission is denied
     */
    fun checkCameraPermission(
        onGranted: () -> Unit,
        onDenied: ((String) -> Unit)? = null
    ) {
        cameraPermissionCallback = onGranted
        permissionDeniedCallback = onDenied

        when {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                onGranted()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    /**
     * Check storage permission and request if needed.
     * Handles Android 13+ (READ_MEDIA_IMAGES) and older versions (READ_EXTERNAL_STORAGE).
     * @param onGranted Callback when permission is granted
     * @param onDenied Optional callback when permission is denied
     */
    fun checkStoragePermission(
        onGranted: () -> Unit,
        onDenied: ((String) -> Unit)? = null
    ) {
        storagePermissionCallback = onGranted
        permissionDeniedCallback = onDenied

        val permission = getStoragePermission()

        when {
            ContextCompat.checkSelfPermission(
                activity,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                onGranted()
            }
            else -> {
                storagePermissionLauncher.launch(permission)
            }
        }
    }

    /**
     * Returns the appropriate storage permission based on Android version.
     */
    private fun getStoragePermission(): String {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
}
