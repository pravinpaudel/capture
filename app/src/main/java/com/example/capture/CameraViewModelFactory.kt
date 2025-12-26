package com.example.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory for creating CameraViewModel with dependencies.
 * Required because ViewModel has constructor parameters.
 */
class CameraViewModelFactory(
    private val ocrProcessor: OcrProcessor
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CameraViewModel::class.java)) {
            return CameraViewModel(ocrProcessor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
