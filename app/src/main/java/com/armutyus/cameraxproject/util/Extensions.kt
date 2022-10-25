package com.armutyus.cameraxproject.util

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import android.webkit.MimeTypeMap
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.Metadata
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import com.armutyus.cameraxproject.util.Util.Companion.FILENAME
import com.armutyus.cameraxproject.util.Util.Companion.PHOTO_EXTENSION
import com.armutyus.cameraxproject.util.Util.Companion.RATIO_16_9_VALUE
import com.armutyus.cameraxproject.util.Util.Companion.RATIO_4_3_VALUE
import com.armutyus.cameraxproject.util.Util.Companion.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.Collections.max
import java.util.Collections.min
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { future ->
            future.addListener({
                val cameraProvider = future.get()
                continuation.resume(cameraProvider)
            }, executor)
        }
    }

val Context.executor: Executor
    get() = ContextCompat.getMainExecutor(this)

suspend fun ImageCapture.takePicture(
    context: Context,
    lensFacing: Int,
    onError: (ImageCaptureException) -> Unit
): Uri? {
    val outputDirectory = context.getOutputDirectory("Photo")
    // Create output file to hold the image
    val photoFile = withContext(Dispatchers.IO) {
        createFile(outputDirectory, FILENAME, PHOTO_EXTENSION)
    }

    return suspendCoroutine { continuation ->
        val outputFileOptions = getOutputFileOptions(lensFacing, photoFile)

        takePicture(
            outputFileOptions,
            Executors.newSingleThreadExecutor(),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                    // If the folder selected is an external media directory, this is
                    // unnecessary but otherwise other apps will not be able to access our
                    // images unless we scan them using [MediaScannerConnection]
                    val mimeType = MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(savedUri.toFile().extension)
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(savedUri.toFile().absolutePath),
                        arrayOf(mimeType)
                    ) { _, uri ->
                        Log.d(TAG, "Image capture scanned into media store: $uri")
                    }
                    continuation.resume(savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            }
        )
    }
}

fun getOutputFileOptions(
    lensFacing: Int,
    photoFile: File
): ImageCapture.OutputFileOptions {
    // Setup image capture metadata
    val metadata = Metadata().apply {
        // Mirror image when using the front camera
        isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
    }
    // Create output options object which contains file + metadata
    return ImageCapture.OutputFileOptions.Builder(photoFile)
        .setMetadata(metadata)
        .build()
}

fun createFile(baseFolder: File, format: String, extension: String) =
    File(
        baseFolder, SimpleDateFormat(format, Locale.getDefault())
            .format(System.currentTimeMillis()) + extension
    )


fun Context.getOutputDirectory(directory: String): File {
    val mediaDir = this.getExternalFilesDir("cameraXproject")?.let {
        File(it, directory).apply { mkdirs() }
    }
    return if (mediaDir != null && mediaDir.exists())
        mediaDir else this.filesDir
}

fun View.vibrate(feedbackConstant: Int) {
    // Either this needs to be set to true, or android:hapticFeedbackEnabled="true" needs to be set in XML
    isHapticFeedbackEnabled = true
    // Most of the constants are off by default: for example, clicking on a button doesn't cause the phone to vibrate anymore
    // if we still want to access this vibration, we'll have to ignore the global settings on that.
    performHapticFeedback(feedbackConstant, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
}


fun aspectRatio(width: Int, height: Int): Int {
    val previewRatio = max(width, height).toDouble() / min(width, height)
    if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
        return AspectRatio.RATIO_4_3
    }
    return AspectRatio.RATIO_16_9
}


