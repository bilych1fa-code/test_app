package com.test.app.viewModels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.test.app.repositories.AnalyticsTracker
import com.test.app.states.WebViewContentUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WebViewContentViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(WebViewContentUiState())
    val uiState: StateFlow<WebViewContentUiState> = _uiState.asStateFlow()

    private var initialDomain: String? = null
    private var hasLoadedFirstPage = false

    fun loadUrl(url: String) {
        if (!isValidUrl(url)) {
            showError("Invalid URL format")
            viewModelScope.launch {
                analyticsRepository.log("error: Invalid URL format - $url")
            }
            return
        }

        if (initialDomain == null) {
            initialDomain = extractDomain(url)
        }

        viewModelScope.launch {
            analyticsRepository.log("load_url: $url")
            _uiState.update {
                it.copy(
                    urlToLoad = url,
                    currentUrl = url,
                    isLoading = true
                )
            }
        }
    }

    fun shouldRedirectToExternalBrowser(newUrl: String): Boolean {
        if (!hasLoadedFirstPage) {
            return false
        }

        val uri = Uri.parse(newUrl)
        if (uri.scheme != "http" && uri.scheme != "https") {
            return false
        }

        if (newUrl.startsWith("http://", ignoreCase = true)) {
            Log.d(TAG, "Redirecting HTTP to external: $newUrl")
            return true
        }

        val newDomain = extractDomain(newUrl)
        val shouldRedirect = newDomain != null && initialDomain != null && newDomain != initialDomain

        if (shouldRedirect) {
            Log.d(TAG, "Different domain detected: $initialDomain -> $newDomain")
        }

        return shouldRedirect
    }

    fun onPageStarted(url: String) {
        Log.d(TAG, "Page started loading: $url")
        viewModelScope.launch {
            analyticsRepository.log("page_started: $url")
        }
        _uiState.update { it.copy(isLoading = true, currentUrl = url) }
    }

    fun onPageFinished(url: String, title: String? = null) {
        Log.d(TAG, "Page finished loading: $url, title: $title")
        hasLoadedFirstPage = true
        viewModelScope.launch {
            analyticsRepository.log("url_loaded: $url")
        }
        _uiState.update {
            it.copy(
                isLoading = false,
                currentUrl = url,
                pageTitle = title
            )
        }
    }

    fun logWarning(message: String) {
        Log.w(TAG, message)
        viewModelScope.launch {
            analyticsRepository.log("warning: $message")
        }
        showError(message)
    }

    fun logExternalNavigation(url: String) {
        Log.d(TAG, "External navigation to: $url")
        viewModelScope.launch {
            analyticsRepository.log("external_navigation: $url")
        }
    }

    fun log(message: String) {
        viewModelScope.launch {
            analyticsRepository.log(message)
        }
    }

    fun onUrlLoaded() {
        _uiState.update { it.copy(urlToLoad = null) }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message, isLoading = false) }
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            uri.scheme != null && uri.host != null
        } catch (e: Exception) {
            false
        }
    }

    fun extractDomain(url: String): String? {
        return try {
            Uri.parse(url).host
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "WebViewContentViewModel"
    }
}