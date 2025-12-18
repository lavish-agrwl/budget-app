package com.example.budget.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

class ThemeViewModel : ViewModel() {
    private val _theme = MutableStateFlow(AppTheme.SYSTEM)
    val theme = _theme.asStateFlow()

    fun toggleTheme() {
        _theme.value = when (_theme.value) {
            AppTheme.SYSTEM -> AppTheme.LIGHT
            AppTheme.LIGHT -> AppTheme.DARK
            AppTheme.DARK -> AppTheme.SYSTEM
        }
    }
}
