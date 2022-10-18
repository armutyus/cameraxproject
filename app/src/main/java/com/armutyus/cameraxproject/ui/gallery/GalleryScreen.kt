package com.armutyus.cameraxproject.ui.gallery

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.armutyus.cameraxproject.ui.theme.CameraXProjectTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(navController: NavController) {
    val context = LocalContext.current
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("camera_screen") }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Open Camera")
            }
        },
        bottomBar = {
            BottomAppBar {
                IconButton(onClick = {
                    Toast.makeText(context, "Search", Toast.LENGTH_LONG).show()
                }) {
                    Icon(imageVector = Icons.Sharp.Search, contentDescription = "Search")
                }
            }
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            TestText(string = "Gallery Screen")
        }
    }
}

@Composable
fun TestText(string: String) {
    Column(
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = string, fontSize = 24.sp, textAlign = TextAlign.Center)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreview() {
    val context = LocalContext.current
    CameraXProjectTheme {
        GalleryScreen(navController = NavController(context))
    }
}