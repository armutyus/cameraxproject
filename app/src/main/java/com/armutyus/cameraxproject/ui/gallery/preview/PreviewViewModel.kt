package com.armutyus.cameraxproject.ui.gallery.preview

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.armutyus.cameraxproject.R
import com.armutyus.cameraxproject.ui.gallery.preview.editmedia.ImageFilters
import com.armutyus.cameraxproject.ui.gallery.preview.editmedia.models.ImageFilter
import com.armutyus.cameraxproject.ui.gallery.preview.models.PreviewScreenEvent
import com.armutyus.cameraxproject.ui.gallery.preview.models.PreviewScreenState
import com.armutyus.cameraxproject.util.Util.Companion.GALLERY_ROUTE
import kotlinx.coroutines.launch
import java.io.File

class PreviewViewModel constructor(private val navController: NavController) : ViewModel() {

    private val _previewScreenState: MutableLiveData<PreviewScreenState> =
        MutableLiveData(PreviewScreenState())
    val previewScreenState: LiveData<PreviewScreenState> = _previewScreenState

    fun onEvent(previewScreenEvent: PreviewScreenEvent) {
        when (previewScreenEvent) {
            is PreviewScreenEvent.ShareTapped -> onShareTapped(
                previewScreenEvent.context,
                previewScreenEvent.file
            )
            is PreviewScreenEvent.DeleteTapped -> onDeleteTapped(previewScreenEvent.file)
            is PreviewScreenEvent.FullScreenToggleTapped -> onFullScreenToggleTapped(
                previewScreenEvent.isFullScreen
            )
            is PreviewScreenEvent.ChangeBarState -> onChangeBarState(previewScreenEvent.zoomState)
            is PreviewScreenEvent.HideController -> hideController(previewScreenEvent.isPlaying)
            PreviewScreenEvent.EditTapped -> onEditTapped()
            PreviewScreenEvent.PlayerViewTapped -> onPlayerViewTapped()
            PreviewScreenEvent.NavigateBack -> onNavigateBack()
        }
    }

    private fun onEditTapped() = viewModelScope.launch {
        _previewScreenState.value = _previewScreenState.value!!.copy(
            isInEditMode = true,
            showBars = false
        )
    }

    private fun onDeleteTapped(file: File) = viewModelScope.launch {
        file.delete()
        navigateTo(GALLERY_ROUTE)
    }

    private fun onShareTapped(context: Context, file: File) = viewModelScope.launch {
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

    private fun onFullScreenToggleTapped(isFullScreen: Boolean) = viewModelScope.launch {
        _previewScreenState.value = _previewScreenState.value!!
            .copy(
                isFullScreen = !isFullScreen,
                showBars = isFullScreen,
                showMediaController = isFullScreen
            )
    }

    private fun onPlayerViewTapped() = viewModelScope.launch {
        val newValue = !(_previewScreenState.value!!.showBars && _previewScreenState.value!!.showMediaController)
        _previewScreenState.value = _previewScreenState.value!!.copy(showBars = newValue, showMediaController = newValue)
    }

    private fun onChangeBarState(zoomState: Boolean) = viewModelScope.launch {
        if (zoomState) {
            _previewScreenState.value = _previewScreenState.value?.copy(showBars = false)
        } else {
            val newValue = !_previewScreenState.value!!.showBars
            _previewScreenState.value = _previewScreenState.value?.copy(showBars = newValue)
        }
    }

    private fun hideController(isPlaying: Boolean) = viewModelScope.launch {
        _previewScreenState.value =
            _previewScreenState.value?.copy(showMediaController = !isPlaying, showBars = !isPlaying)
    }

    //EditMedia Works

    private val _imageFilterList: MutableLiveData<List<ImageFilter>> = MutableLiveData(emptyList())
    val imageFilterList: LiveData<List<ImageFilter>> = _imageFilterList

    private val _currentImageBitmap: MutableLiveData<Bitmap> = MutableLiveData()
    val currentImageBitmap: LiveData<Bitmap> = _currentImageBitmap

    private val _filteredBitmap: MutableLiveData<Bitmap?> = MutableLiveData()
    val filteredBitmap: LiveData<Bitmap?> = _filteredBitmap

    private val _imageHasFilter: MutableLiveData<Boolean> = MutableLiveData(false)
    val imageHasFilter: LiveData<Boolean> = _imageHasFilter

    fun loadImageFilters(context: Context, originalImageUri: Uri) = viewModelScope.launch {
        @Suppress("DEPRECATION")
        _currentImageBitmap.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, originalImageUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, originalImageUri)
        }
        val image = getPreviewImage(originalImage = _currentImageBitmap.value!!)
        val imageFilterList = ImageFilters.ImageFiltersCompat.getImageFiltersList(context, image)
        _imageFilterList.value = imageFilterList
    }

    private fun getPreviewImage(originalImage: Bitmap): Bitmap {
        return kotlin.runCatching {
            val previewWidth = 150
            val previewHeight = originalImage.height * previewWidth / originalImage.width
            Bitmap.createScaledBitmap(originalImage, previewWidth, previewHeight, false)
        }.getOrDefault(originalImage)
    }

    fun selectedFilter(filterName: String){
        _imageHasFilter.value = filterName != "Normal"
    }

    fun setFilteredBitmap(imageBitmap: Bitmap) {
        _filteredBitmap.value = imageBitmap
    }

    //EditMedia End

    private fun navigateTo(route: String) = viewModelScope.launch {
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    private fun onNavigateBack() = viewModelScope.launch {
        navController.popBackStack()
    }

}