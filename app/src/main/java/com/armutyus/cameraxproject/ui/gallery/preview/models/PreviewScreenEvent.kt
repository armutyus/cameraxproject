package com.armutyus.cameraxproject.ui.gallery.preview.models

import android.content.Context
import java.io.File

sealed class PreviewScreenEvent {

    data class ShareTapped(val context: Context) : PreviewScreenEvent()
    data class DeleteTapped(val file: File) : PreviewScreenEvent()

    object EditTapped : PreviewScreenEvent()
}