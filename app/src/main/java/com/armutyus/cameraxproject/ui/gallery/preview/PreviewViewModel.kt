package com.armutyus.cameraxproject.ui.gallery.preview

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutyus.cameraxproject.R
import com.armutyus.cameraxproject.ui.gallery.preview.models.PlaybackStatus
import com.armutyus.cameraxproject.ui.gallery.preview.models.PreviewScreenEffect
import com.armutyus.cameraxproject.ui.gallery.preview.models.PreviewScreenEvent
import com.armutyus.cameraxproject.ui.gallery.preview.models.PreviewScreenState
import com.armutyus.cameraxproject.util.Util.Companion.GALLERY_ROUTE
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class PreviewViewModel : ViewModel() {

    private val _previewScreenState = MutableStateFlow(PreviewScreenState())
    val previewScreenState: StateFlow<PreviewScreenState> = _previewScreenState

    private val _previewEffect = MutableSharedFlow<PreviewScreenEffect>()
    val previewEffect: SharedFlow<PreviewScreenEffect> = _previewEffect

    fun onEvent(previewScreenEvent: PreviewScreenEvent) {
        when (previewScreenEvent) {
            is PreviewScreenEvent.ShareTapped -> onShareTapped(
                previewScreenEvent.context,
                previewScreenEvent.file
            )
            is PreviewScreenEvent.DeleteTapped -> onDeleteTapped(previewScreenEvent.file)
            is PreviewScreenEvent.OnProgress -> onProgress(previewScreenEvent.progress)

            PreviewScreenEvent.EditTapped -> onEditTapped()
            PreviewScreenEvent.Completed -> onCompleted()
            PreviewScreenEvent.PauseTapped -> onPauseTapped()
            PreviewScreenEvent.PlayTapped -> onPlayTapped()
            PreviewScreenEvent.Prepared -> onPrepared()
        }
    }

    private fun onEditTapped() {
        TODO("Not yet implemented")
    }

    private fun onDeleteTapped(file: File) {
        viewModelScope.launch {
            file.delete()
            _previewEffect.emit(PreviewScreenEffect.NavigateTo(GALLERY_ROUTE))
        }
    }

    private fun onShareTapped(context: Context, file: File) {
        viewModelScope.launch {
            if (file.exists()) {
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "com.armutyus.cameraxproject.fileprovider",
                    file
                )
                val shareIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "*/*"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
                try {
                    context.startActivity(
                        Intent.createChooser(
                            shareIntent,
                            context.getString(R.string.share)
                        )
                    )
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, R.string.no_app_available, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, R.string.file_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onPauseTapped() {
        viewModelScope.launch {
            _previewEffect.emit(PreviewScreenEffect.Pause)
        }
        _previewScreenState.update { it.copy(playbackStatus = PlaybackStatus.Idle) }
    }

    private fun onPlayTapped() {
        viewModelScope.launch {
            _previewEffect.emit(PreviewScreenEffect.Play)
        }
    }

    private fun onProgress(progress: Int) {
        _previewScreenState.update {
            it.copy(
                playbackPosition = progress,
                playbackStatus = PlaybackStatus.InProgress
            )
        }
    }

    private fun onPrepared() {
        _previewScreenState.update { it.copy(playbackStatus = PlaybackStatus.Idle) }
    }

    private fun onCompleted() {
        _previewScreenState.update {
            it.copy(
                playbackStatus = PlaybackStatus.Idle,
                playbackPosition = 0
            )
        }
    }

}