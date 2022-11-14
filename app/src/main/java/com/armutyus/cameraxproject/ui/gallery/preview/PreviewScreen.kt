package com.armutyus.cameraxproject.ui.gallery.preview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.armutyus.cameraxproject.R
import com.armutyus.cameraxproject.ui.gallery.GalleryViewModel
import com.armutyus.cameraxproject.ui.gallery.models.BottomNavItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    filePath: String,
    itemType: String,
    navController: NavController,
    factory: ViewModelProvider.Factory,
    previewViewModel: PreviewViewModel = viewModel(factory = factory),
    galleryViewModel: GalleryViewModel = viewModel(factory = factory),
    onShowMessage: (message: String) -> Unit
) {
    val state by previewViewModel.previewScreenState.collectAsState()

    val bottomNavItems = listOf(
        BottomNavItem.Share,
        BottomNavItem.EditItem,
        BottomNavItem.Delete
    )

    Scaffold(
        topBar = {
            if (state.showBars)
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Sharp.ArrowBack,
                                contentDescription = stringResource(id = R.string.cancel)
                            )
                        }
                    },
                    title = { Text(text = stringResource(id = R.string.select)/*item taken time*/) }
                )
        },
        bottomBar = {
            NavigationBar {
                if (!state.showBars) {
                    bottomNavItems.forEach { bottomNavItem ->
                        NavigationBarItem(
                            selected = false,
                            icon = {
                                Icon(
                                    imageVector = bottomNavItem.icon,
                                    contentDescription = stringResource(id = bottomNavItem.label)
                                )
                            },
                            label = {
                                Text(text = stringResource(id = bottomNavItem.label))
                            },
                            alwaysShowLabel = false,
                            onClick = { /*onEvent*/ }
                        )
                    }
                }
            }
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            if (itemType == "photo") {
                AsyncImage(
                    model = filePath,
                    modifier = Modifier.fillMaxSize(),
                    filterQuality = FilterQuality.High,
                    contentDescription = ""
                )
            } else {
                //VideoContent
            }
        }
    }
}