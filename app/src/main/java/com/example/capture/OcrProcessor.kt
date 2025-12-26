package com.example.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException
import kotlinx.coroutines.tasks.await
import kotlin.math.max


class OcrProcessor(private val context: Context) {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    companion object {
        private const val MAX_IMAGE_DIMENSION = 1024
    }

    private fun compressImage(imageUri: Uri): Bitmap? {
        return try {
            // Opens the image file as a stream of bytes
            val inputStream = context.contentResolver.openInputStream(imageUri) ?: return null

            // Super Imp Trick:- Don’t load the image into memory. Just read its width and height
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            // Calculate inSampleSize
            val width = options.outWidth
            val height = options.outHeight
            var inSampleSize = 1 // controls how much the image will be scaled down.

            if (width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION) {
                val maxDimension = max(width, height)
                inSampleSize = maxDimension / MAX_IMAGE_DIMENSION
            }

            // Decode with inSampleSize
            val inputStream2 = context.contentResolver.openInputStream(imageUri) // Opens the image file as a stream of bytes (1st stream was already consumed)
            val finalOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            // Decodes the image file into a Bitmap (smaller size)
            val bitmap = BitmapFactory.decodeStream(inputStream2, null, finalOptions)
            inputStream2?.close()

            bitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Processes an image URI and extracts text using ML Kit Text Recognition
     * @param imageUri URI of the image to process
     * @return Result containing extracted text or error
     */
    suspend fun extractText(imageUri: Uri): Result<List<Text.TextBlock>> { // coroutine function (similar to async)
        return try {
            // Compress image first
            val compressedBitmap = compressImage(imageUri)
                ?: return Result.failure(Exception("Failed to load and compress image"))

            val inputImage = InputImage.fromFilePath(context, imageUri)
            val visionText = textRecognizer.process(inputImage).await()

            val extractedTextBlock = visionText.textBlocks
            if (extractedTextBlock.isNotEmpty()) {
                Result.success(extractedTextBlock)
            } else {
                Result.failure(Exception("Could not extract text"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Failed to load image: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("OCR processing failed: ${e.message}"))
        }
    }

    /**
     * Releases resources used by the text recognizer
     * Call this when done with OCR processing
     */
    fun close() {
        textRecognizer.close()
    }

    suspend fun extractAndParseEvent(imageUri: Uri): Result<RawEventData> {
        return try {
            val extractedTextBlock = extractText(imageUri)

            if(extractedTextBlock.isSuccess) {
                val parser = EventParser()
                val eventData = parser.parse(extractedTextBlock.getOrThrow())
                Result.success(eventData)
            } else {
                Result.failure(extractedTextBlock.exceptionOrNull() ?: Exception("Unknown error"))
            }

        } catch (e: Exception) {
            Result.failure(Exception("Failed to parse event data: ${e.message}"))
        }
    }
}