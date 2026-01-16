package com.test.app.states

data class WebViewContentUiState(
    val isLoading: Boolean = false,
    val currentUrl: String = "",
    val pageTitle: String? = null,
    val urlToLoad: String? = null,
    val errorMessage: String? = null
)