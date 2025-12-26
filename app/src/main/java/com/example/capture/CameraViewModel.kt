package com.example.capture

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * ViewModel for camera and OCR operations.
 * Manages business logic, state, and coordinates between UI and data layer.
 * Survives configuration changes (e.g., screen rotation).
 */
class CameraViewModel(
    private val ocrProcessor: OcrProcessor
) : ViewModel() {

    // LiveData for processing state
    private val _isProcessing = MutableLiveData<Boolean>(false)
    val isProcessing: LiveData<Boolean> = _isProcessing

    // LiveData for OCR result
    private val _ocrResult = MutableLiveData<Result<RawEventData>>()
    val ocrResult: LiveData<Result<RawEventData>> = _ocrResult

    // LiveData for captured/selected image URI
    private val _imageUri = MutableLiveData<Uri?>()
    val imageUri: LiveData<Uri?> = _imageUri

    /**
     * Process an image with OCR and parse event data.
     * @param uri The image URI to process
     */
    fun processImageWithOCR(uri: Uri) {
        _imageUri.value = uri
        _isProcessing.value = true

        viewModelScope.launch {
            try {
                val eventData = ocrProcessor.extractAndParseEvent(uri)
                _ocrResult.value = eventData
            } catch (e: Exception) {
                _ocrResult.value = Result.failure(e)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Set the current image URI (for camera capture or gallery selection).
     */
    fun setImageUri(uri: Uri) {
        _imageUri.value = uri
    }

    /**
     * Clean up resources when ViewModel is destroyed.
     */
    override fun onCleared() {
        super.onCleared()
        ocrProcessor.close()
    }
}
