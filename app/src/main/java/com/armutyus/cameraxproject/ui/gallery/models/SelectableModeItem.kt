package com.armutyus.cameraxproject.ui.gallery.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Cancel
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.icons.sharp.Share
import androidx.compose.ui.graphics.vector.ImageVector
import com.armutyus.cameraxproject.R


sealed class SelectableModeItem(var icon: ImageVector, var label: Int) {

    object Cancel : SelectableModeItem(Icons.Sharp.Cancel, R.string.cancel)
    object Delete : SelectableModeItem(Icons.Sharp.Delete, R.string.delete)
    object Share : SelectableModeItem(Icons.Sharp.Share, R.string.share)
}
