package com.armutyus.cameraxproject.ui.gallery.preview.models

import android.content.Context
import java.io.File

sealed class PreviewScreenEvent {

    data class ShareTapped(val context: Context, val file: File) : PreviewScreenEvent()
    data class DeleteTapped(val file: File) : PreviewScreenEvent()
    object EditTapped : PreviewScreenEvent()

    object PlayTapped : PreviewScreenEvent()
    object PauseTapped : PreviewScreenEvent()

    object Prepared : PreviewScreenEvent()
    object Completed : PreviewScreenEvent()
    data class OnProgress(val progress: Int) : PreviewScreenEvent()
}