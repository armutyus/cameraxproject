package com.armutyus.cameraxproject.ui.camera

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowBack
import androidx.compose.material.icons.sharp.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.armutyus.cameraxproject.ui.theme.CameraXProjectTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(navController: NavController) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(title = { CameraScreenText(string = "Camera") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Sharp.ArrowBack,
                            contentDescription = "Back to previous screen"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(
                            imageVector = Icons.Sharp.Settings,
                            contentDescription = "Go to settings"
                        )
                    }
                })
        }
    ) {
        Surface(
            modifier = Modifier
                .padding(it)
        ) {
            CameraContent(Modifier.fillMaxSize())
        }
    }
}

@Composable
fun CameraContent(modifier: Modifier = Modifier) {
    val emptyImageUri = Uri.parse("file://dev/null")
    var imageUri by remember { mutableStateOf(emptyImageUri) }
    if (imageUri != emptyImageUri) {
        Box(modifier = modifier) {
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = rememberAsyncImagePainter(imageUri),
                contentDescription = "Captured image"
            )
            Button(
                modifier = Modifier.align(Alignment.BottomCenter),
                onClick = {
                    imageUri = emptyImageUri
                }
            ) {
                Text("Remove image")
            }
        }
    } else {
        CameraCapture(
            modifier = modifier,
            onImageFile = { file ->
                imageUri = file.toUri()
            }
        )
    }
}

@Composable
fun CameraScreenText(string: String) {
    Column(
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = string, fontSize = 16.sp, textAlign = TextAlign.Center)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreview() {
    val context = LocalContext.current
    CameraXProjectTheme {
        CameraScreen(navController = NavController(context))
    }
}