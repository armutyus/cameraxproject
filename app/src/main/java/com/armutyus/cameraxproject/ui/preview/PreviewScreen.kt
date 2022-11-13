package com.armutyus.cameraxproject.ui.preview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage

@Composable
fun PreviewScreen(
    filePath: String,
    navController: NavController,
    factory: ViewModelProvider.Factory,
    galleryViewModel: PreviewViewModel = viewModel(factory = factory),
    onShowMessage: (message: String) -> Unit
) {
    AsyncImage(model = filePath, modifier = Modifier.fillMaxSize(), filterQuality = FilterQuality.High, contentDescription = "")
}