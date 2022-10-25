package com.armutyus.cameraxproject.ui.photo

import android.content.Context
import android.content.res.Configuration
import android.media.MediaScannerConnection
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.armutyus.cameraxproject.util.PreviewState
import com.armutyus.cameraxproject.util.Util
import com.armutyus.cameraxproject.util.Util.Companion.TAG
import com.armutyus.cameraxproject.util.aspectRatio
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.util.concurrent.Executors
import kotlin.coroutines.resume

class PhotoCaptureManager private constructor(private val builder: Builder) :
    LifecycleEventObserver {

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var imageCapture: ImageCapture

    var photoListener: PhotoListener = object : PhotoListener {
        override fun onInitialised(cameraLensInfo: HashMap<Int, CameraInfo>) {}
        override fun onSuccess(imageResult: ImageCapture.OutputFileResults) {}
        override fun onError(exception: Exception) {}
    }

    init {
        getLifecycle().addObserver(this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                cameraProviderFuture = ProcessCameraProvider.getInstance(getContext())
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    queryCameraInfo(source, cameraProvider)
                }, ContextCompat.getMainExecutor(getContext()))
            }
            else -> Unit
        }
    }

    private fun getCameraPreview() = PreviewView(getContext()).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        keepScreenOn = true
    }

    private fun getLifecycle() = builder.lifecycleOwner?.lifecycle!!

    private fun getContext() = builder.context

    private fun getLifeCycleOwner() = builder.lifecycleOwner!!

    private fun getConfiguration() = builder.context.resources.configuration

    /**
     * Queries the capabilities of the FRONT and BACK camera lens
     * The result is stored in an array map.
     *
     * With this, we can determine if a camera lens is available or not,
     * and what capabilities the lens can support e.g flash support
     */
    private fun queryCameraInfo(
        lifecycleOwner: LifecycleOwner,
        cameraProvider: ProcessCameraProvider
    ) {
        val cameraLensInfo = HashMap<Int, CameraInfo>()
        arrayOf(CameraSelector.LENS_FACING_BACK, CameraSelector.LENS_FACING_FRONT).forEach { lens ->
            val cameraSelector = CameraSelector.Builder().requireLensFacing(lens).build()
            if (cameraProvider.hasCamera(cameraSelector)) {
                val camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector)
                if (lens == CameraSelector.LENS_FACING_BACK) {
                    cameraLensInfo[CameraSelector.LENS_FACING_BACK] = camera.cameraInfo
                } else if (lens == CameraSelector.LENS_FACING_FRONT) {
                    cameraLensInfo[CameraSelector.LENS_FACING_FRONT] = camera.cameraInfo
                }
            }
        }
        photoListener.onInitialised(cameraLensInfo)
    }

    /**
     * Takes a [previewState] argument to determine the camera options
     *
     * Create a Preview.
     * Create Image Capture use case
     * Bind the selected camera and any use cases to the lifecycle.
     * Connect the Preview to the PreviewView.
     */
    @Synchronized
    private fun showPreview(previewState: PreviewState, cameraPreview: PreviewView): View {
        getLifeCycleOwner().lifecycleScope.launchWhenResumed {
            val cameraProvider = cameraProviderFuture.get()

            // Get screen metrics used to setup camera for full screen resolution
            val metrics = Util.ScreenSizeCompat.getScreenSize(getContext())
            Log.d(TAG, "Screen metrics: ${metrics.width} x ${metrics.height}")
            val screenAspectRatio = aspectRatio(metrics.width, metrics.height)
            Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

            val rotation = getConfiguration().orientation

            //Select a camera lens
            val cameraSelector: CameraSelector = CameraSelector.Builder()
                .requireLensFacing(previewState.cameraLens)
                .build()

            //Create Preview use case
            val preview: Preview = Preview.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build()
                .apply { setSurfaceProvider(cameraPreview.surfaceProvider) }

            //Create Image Capture use case
            imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .setFlashMode(previewState.flashMode)
                .build()

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                getLifeCycleOwner(),
                cameraSelector,
                preview,
                imageCapture
            )
        }
        return cameraPreview
    }

    fun showPreview(previewState: PreviewState): View {
        return showPreview(previewState, getCameraPreview())
    }

    fun updatePreview(previewState: PreviewState, previewView: View) {
        showPreview(previewState, previewView as PreviewView)
    }

    fun takePhoto(filePath: String, previewState: PreviewState) {
        val photoFile = File(filePath)
        val outputFileOptions = getOutputFileOptions(previewState, photoFile)

        imageCapture.takePicture(
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
                        getContext(),
                        arrayOf(savedUri.toFile().absolutePath),
                        arrayOf(mimeType)
                    ) { _, uri ->
                        Log.d(Util.TAG, "Image capture scanned into media store: $uri")
                    }
                    photoListener.onSuccess(output)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, exception.localizedMessage ?: "Image capture error")
                    photoListener.onError(exception)
                }
            }
        )
    }

    private fun getOutputFileOptions(
        previewState: PreviewState,
        photoFile: File
    ): ImageCapture.OutputFileOptions {
        // Setup image capture metadata
        val metadata = ImageCapture.Metadata().apply {
            // Mirror image when using the front camera
            isReversedHorizontal = previewState.cameraLens == CameraSelector.LENS_FACING_FRONT
        }
        // Create output options object which contains file + metadata
        return ImageCapture.OutputFileOptions.Builder(photoFile)
            .setMetadata(metadata)
            .build()
    }

    class Builder(val context: Context) {

        var lifecycleOwner: LifecycleOwner? = null
            private set

        fun registerLifecycleOwner(source: LifecycleOwner): Builder {
            this.lifecycleOwner = source
            return this
        }

        fun create(): PhotoCaptureManager {
            requireNotNull(lifecycleOwner) { "Lifecycle owner is not set" }
            return PhotoCaptureManager(this)
        }
    }

    interface PhotoListener {
        fun onInitialised(cameraLensInfo: HashMap<Int, CameraInfo>)
        fun onSuccess(imageResult: ImageCapture.OutputFileResults)
        fun onError(exception: Exception)
    }
}

val LocalPhotoCaptureManager = compositionLocalOf<PhotoCaptureManager> { error("No capture manager found!") }