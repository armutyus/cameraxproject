package com.armutyus.cameraxproject.ui.gallery.preview

import androidx.lifecycle.ViewModel
import com.armutyus.cameraxproject.ui.gallery.preview.models.PreviewScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PreviewViewModel : ViewModel() {

    private val _previewScreenState = MutableStateFlow(PreviewScreenState())
    val previewScreenState: StateFlow<PreviewScreenState> = _previewScreenState

}