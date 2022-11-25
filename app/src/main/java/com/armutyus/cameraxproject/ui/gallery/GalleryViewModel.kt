package com.armutyus.cameraxproject.ui.gallery

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutyus.cameraxproject.R
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

    private val _selectedItems = MutableStateFlow(mutableListOf<MediaItem>())
    val selectedItems: StateFlow<List<MediaItem>> = _selectedItems

    private val _galleryEffect = MutableSharedFlow<GalleryEffect>()
    val galleryEffect: SharedFlow<GalleryEffect> = _galleryEffect

    fun onEvent(galleryEvent: GalleryEvent) {
        when (galleryEvent) {
            is GalleryEvent.ItemClicked -> onItemClicked(galleryEvent.item)
            is GalleryEvent.ShareTapped -> onShareTapped(galleryEvent.context)

            GalleryEvent.FabClicked -> onFabClicked()
            GalleryEvent.SelectAllClicked -> changeSelectAllState()
            GalleryEvent.ItemLongClicked -> onItemLongClicked()
            GalleryEvent.CancelSelectableMode -> cancelSelectableMode()
            GalleryEvent.DeleteTapped -> deleteSelectedItems()
        }
    }

    init {
        loadMedia()
    }

    private fun loadMedia() {
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

            media.clear()
            media.addAll(photos + videos)

            val groupedMedia = media.sortedByDescending { it.takenTime }.groupBy { it.takenTime }

            _mediaItems.value += groupedMedia
        }
    }

    private fun onFabClicked() {
        cancelSelectableMode()
        viewModelScope.launch {
            _galleryEffect.emit(GalleryEffect.NavigateTo(PHOTO_ROUTE))
        }
    }

    private fun changeSelectAllState() {
        if (_galleryState.value.selectAllClicked) {
            _galleryState.update {
                it.copy(selectAllClicked = false)
            }
        } else {
            _galleryState.update {
                it.copy(selectAllClicked = true)
            }
        }
    }

    fun onSelectAllClicked(checked: Boolean) {
        viewModelScope.launch {
            _selectedItems.value.clear()
            if (checked) {
                _mediaItems.value.forEach {
                    it.value.forEach { mediaItem ->
                        mediaItem.selected = true
                        _selectedItems.value.add(mediaItem)
                    }
                }
            } else {
                _mediaItems.value.forEach {
                    it.value.forEach { mediaItem ->
                        mediaItem.selected = false
                        _selectedItems.value.clear()
                    }
                }
            }
        }
    }

    private fun onItemClicked(item: MediaItem?) {
        cancelSelectableMode()
        val uri = item?.uri
        viewModelScope.launch {
            _galleryEffect.emit(
                GalleryEffect.NavigateTo("preview_screen/?filePath=${uri?.toString()}")
            )
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
        viewModelScope.launch {
            _selectedItems.value.clear()
            _mediaItems.value.forEach {
                it.value.forEach { mediaItem ->
                    mediaItem.selected = false
                }
            }
            _galleryState.update {
                it.copy(
                    selectableMode = false,
                    selectAllClicked = false
                )
            }
        }
    }

    fun onItemCheckedChange(checked: Boolean, item: MediaItem) {
        viewModelScope.launch {
            if (checked) {
                val checkItem = _selectedItems.value.contains(item)
                if (!checkItem) {
                    item.selected = true
                    _selectedItems.value.add(item)
                }
            } else {
                val checkItem = _selectedItems.value.contains(item)
                if (checkItem) {
                    item.selected = false
                    _selectedItems.value.remove(item)
                }
            }
        }
    }

    fun itemSelectedCheck(item: MediaItem): Boolean {
        return item.selected
    }

    private fun onShareTapped(context: Context) {
        viewModelScope.launch {
            if (_selectedItems.value.isNotEmpty()) {
                val uriList = ArrayList<Uri>()
                _selectedItems.value.forEach {
                    val contentUri = FileProvider.getUriForFile(
                        context,
                        "com.armutyus.cameraxproject.fileprovider",
                        it.uri?.toFile()!!
                    )
                    uriList.add(contentUri)
                }
                val shareIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND_MULTIPLE
                    type = "*/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
                try {
                    context.startActivity(
                        Intent.createChooser(
                            shareIntent,
                            context.getString(R.string.share)
                        )
                    )
                    uriList.clear()
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, R.string.no_app_available, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, R.string.choose_media, Toast.LENGTH_SHORT).show()
            }
        }
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