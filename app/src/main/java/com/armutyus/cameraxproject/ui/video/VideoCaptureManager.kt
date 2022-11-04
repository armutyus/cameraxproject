package com.armutyus.cameraxproject.ui.video

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.runtime.compositionLocalOf
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.armutyus.cameraxproject.ui.video.models.PreviewVideoState
import com.armutyus.cameraxproject.util.Util
import com.google.common.util.concurrent.ListenableFuture
import java.io.File

class VideoCaptureManager private constructor(private val builder: Builder) :
    LifecycleEventObserver {

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var videoCapture: VideoCapture<Recorder>

    private lateinit var activeRecording: Recording

    var listener: Listener? = null

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

    @RequiresApi(Build.VERSION_CODES.R)
    private fun getView() = builder.context.display!!

    /**
     * Using an OrientationEventListener allows you to continuously update the target rotation
     * of the camera use cases as the device’s orientation changes.
     */
    private val orientationEventListener by lazy {
        object : OrientationEventListener(getContext()) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == Util.UNKNOWN_ORIENTATION) {
                    return
                }

                val rotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
            }
        }
    }

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
        listener?.onInitialised(cameraLensInfo)
    }

    /**
     * Takes a [previewState] argument to determine the camera options
     *
     * Create a Preview.
     * Create Video Capture use case
     * Bind the selected camera and any use cases to the lifecycle.
     * Connect the Preview to the PreviewView.
     */
    fun showPreview(
        previewVideoState: PreviewVideoState,
        cameraPreview: PreviewView = getCameraPreview()
    ): View {
        getLifeCycleOwner().lifecycleScope.launchWhenResumed {
            val cameraProvider = cameraProviderFuture.await()
            cameraProvider.unbindAll()

            // Every time the orientation of device changes, update rotation for use cases
            orientationEventListener.enable()

            //Select a camera lens
            val cameraSelector: CameraSelector = CameraSelector.Builder()
                .requireLensFacing(previewVideoState.cameraLens)
                .build()

            //Create Preview use case
            val preview: Preview = Preview.Builder()
                .build()
                .apply { setSurfaceProvider(cameraPreview.surfaceProvider) }

            //Create Video Capture use case
            val recorder = Recorder.Builder().build()
            videoCapture = VideoCapture.withOutput(recorder)

            cameraProvider.bindToLifecycle(
                getLifeCycleOwner(),
                cameraSelector,
                preview,
                videoCapture
            ).apply {
                cameraControl.enableTorch(previewVideoState.torchState == TorchState.ON)
            }
        }
        return cameraPreview
    }

    fun updatePreview(previewVideoState: PreviewVideoState, previewView: View) {
        showPreview(previewVideoState, previewView as PreviewView)
    }

    @SuppressLint("MissingPermission")
    fun startRecording(filePath: String) {
        val outputOptions = FileOutputOptions.Builder(File(filePath)).build()
        activeRecording = videoCapture.output
            .prepareRecording(getContext(), outputOptions)
            .apply { withAudioEnabled() }
            .start(ContextCompat.getMainExecutor(getContext()), videoRecordingListener)
    }

    fun pauseRecording() {
        activeRecording.pause()
        listener?.recordingPaused()
    }

    fun resumeRecording() {
        activeRecording.resume()
    }

    fun stopRecording() {
        activeRecording.stop()
    }

    private val videoRecordingListener = Consumer<VideoRecordEvent> { event ->
        when (event) {
            is VideoRecordEvent.Finalize -> if (event.hasError()) {
                listener?.onError(event.cause)
            } else {
                listener?.recordingCompleted(event.outputResults.outputUri)
            }
            is VideoRecordEvent.Pause -> listener?.recordingPaused()
            is VideoRecordEvent.Status -> {
                listener?.onProgress(event.recordingStats.recordedDurationNanos.fromNanoToSeconds())
            }
        }
    }

    interface Listener {
        fun onInitialised(cameraLensInfo: HashMap<Int, CameraInfo>)
        fun onProgress(progress: Int)
        fun recordingPaused()
        fun recordingCompleted(outputUri: Uri)
        fun onError(throwable: Throwable?)
    }

    class Builder(val context: Context) {

        var lifecycleOwner: LifecycleOwner? = null
            private set

        fun registerLifecycleOwner(source: LifecycleOwner): Builder {
            this.lifecycleOwner = source
            return this
        }

        fun create(): VideoCaptureManager {
            requireNotNull(lifecycleOwner) { "Lifecycle owner is not set" }
            return VideoCaptureManager(this)
        }
    }

    private fun Long.fromNanoToSeconds() = (this / (1000 * 1000 * 1000)).toInt()
}

val LocalVideoCaptureManager =
    compositionLocalOf<VideoCaptureManager> { error("No capture manager found!") }