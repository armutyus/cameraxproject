package com.armutyus.cameraxproject.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import kotlinx.coroutines.launch

open class BaseViewModel(private val navController: NavController): ViewModel() {

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun onNavigateBack() {
        navController.popBackStack()
    }

}