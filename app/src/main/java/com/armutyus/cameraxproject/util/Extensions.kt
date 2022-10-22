package com.armutyus.cameraxproject.util

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.Metadata
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import com.armutyus.cameraxproject.R
import com.armutyus.cameraxproject.util.Util.Companion.FILENAME
import com.armutyus.cameraxproject.util.Util.Companion.PHOTO_EXTENSION
import com.armutyus.cameraxproject.util.Util.Companion.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
    val outputDirectory = context.getOutputDirectory()
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


fun Context.getOutputDirectory(): File {
    val mediaDir = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.let {
        File(it, "cameraXproject").apply { mkdirs() }
    }
    return if (mediaDir != null && mediaDir.exists())
        mediaDir else this.filesDir
}


