package com.armutyus.cameraxproject.ui.gallery

import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutyus.cameraxproject.ui.gallery.models.GalleryEffect
import com.armutyus.cameraxproject.ui.gallery.models.GalleryEvent
import com.armutyus.cameraxproject.ui.gallery.models.GalleryState
import com.armutyus.cameraxproject.ui.gallery.models.MediaItem
import com.armutyus.cameraxproject.util.FileManager
import com.armutyus.cameraxproject.util.Util.Companion.PHOTO_DIR
import com.armutyus.cameraxproject.util.Util.Companion.PHOTO_ROUTE
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_DIR
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GalleryViewModel constructor(private val fileManager: FileManager) : ViewModel() {

    private val _galleryState = MutableStateFlow(GalleryState())
    val galleryState: StateFlow<GalleryState> = _galleryState

    private val _mediaItems = MutableStateFlow(mapOf<String, List<MediaItem>>())
    val mediaItems: StateFlow<Map<String, List<MediaItem>>> = _mediaItems

    private val _photoItems = MutableStateFlow(mapOf<String, List<MediaItem>>())
    val photoItems: StateFlow<Map<String, List<MediaItem>>> = _photoItems

    private val _videoItems = MutableStateFlow(mapOf<String, List<MediaItem>>())
    val videoItems: StateFlow<Map<String, List<MediaItem>>> = _videoItems

    private val _selectedItems = MutableStateFlow(mutableListOf<MediaItem>())
    val selectedItems: StateFlow<List<MediaItem>> = _selectedItems

    private val _galleryEffect = MutableSharedFlow<GalleryEffect>()
    val galleryEffect: SharedFlow<GalleryEffect> = _galleryEffect

    fun onEvent(galleryEvent: GalleryEvent) {
        when (galleryEvent) {
            is GalleryEvent.ItemClicked -> onItemClicked(galleryEvent.uri)
            is GalleryEvent.ItemChecked -> onItemChecked(galleryEvent.item)
            is GalleryEvent.ItemUnchecked -> onItemUnchecked(galleryEvent.item)

            GalleryEvent.FabClicked -> onFabClicked()
            GalleryEvent.SelectAllClicked -> onSelectAllClicked()
            GalleryEvent.ItemLongClicked -> onItemLongClicked()
            GalleryEvent.CancelSelectableMode -> cancelSelectableMode()
            GalleryEvent.DeleteTapped -> deleteSelectedItems()
            GalleryEvent.ShareTapped -> onShareTapped()
        }
    }

    fun loadMedia() {
        viewModelScope.launch {
            val media = mutableListOf<MediaItem>()

            val photoDir = fileManager.getPrivateFileDirectory(PHOTO_DIR)
            val photos = photoDir?.listFiles()?.mapIndexed { _, file ->
                val takenTime = file.name.substring(0, 10).replace("-", "/")
                MediaItem(
                    takenTime,
                    name = file.name,
                    uri = file.toUri(),
                    type = MediaItem.Type.PHOTO
                )
            } as List<MediaItem>

            val videoDir = fileManager.getPrivateFileDirectory(VIDEO_DIR)
            val videos = videoDir?.listFiles()?.mapIndexed { _, file ->
                val takenTime = file.name.substring(0, 10).replace("-", "/")
                MediaItem(
                    takenTime,
                    name = file.name,
                    uri = file.toUri(),
                    type = MediaItem.Type.VIDEO
                )
            } as List<MediaItem>

            media.addAll(photos + videos)

            val groupedMedia = media.sortedByDescending { it.takenTime }.groupBy { it.takenTime }
            val groupedPhotos = photos.sortedByDescending { it.takenTime }.groupBy { it.takenTime }
            val groupedVideos = videos.sortedByDescending { it.takenTime }.groupBy { it.takenTime }

            _mediaItems.value += groupedMedia
            _photoItems.value += groupedPhotos
            _videoItems.value += groupedVideos
        }
    }

    private fun onFabClicked() {
        cancelSelectableMode()
        viewModelScope.launch {
            _galleryEffect.emit(GalleryEffect.NavigateTo(PHOTO_ROUTE))
        }
    }

    private fun onSelectAllClicked() {
        //handle item.selected update
    }

    private fun onItemClicked(uri: Uri?) {
        cancelSelectableMode()
        viewModelScope.launch {
            _galleryEffect.emit(GalleryEffect.NavigateTo("preview_screen/${uri?.toString()}"))
        }
    }

    private fun onItemLongClicked() {
        _galleryState.update {
            it.copy(
                selectableMode = true
            )
        }
    }

    private fun cancelSelectableMode() {
        _galleryState.update {
            it.copy(
                selectableMode = false
            )
        }
    }

    private fun onItemChecked(item: MediaItem) {
        viewModelScope.launch {
            _selectedItems.value.add(item)
            item.selected = true
        }
    }

    private fun onItemUnchecked(item: MediaItem) {
        viewModelScope.launch {
            _selectedItems.value.remove(item)
            item.selected = false
        }
    }

    private fun onShareTapped() {
        //share selected items to other apps
    }

    private fun deleteSelectedItems() {
        viewModelScope.launch {
            _selectedItems.value.forEach {
                it.uri?.toFile()?.delete()
            }
            cancelSelectableMode()
            loadMedia()
        }
    }
}