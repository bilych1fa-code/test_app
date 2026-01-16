package com.test.app.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.test.app.managers.ContentVisibilityManager
import com.test.app.repositories.AnalyticsTracker
import com.test.app.states.MenuUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsTracker,
    private val contentVisibilityManager: ContentVisibilityManager
) : ViewModel() {

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _uiState = MutableStateFlow(MenuUiState())
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    init {
        loadLogs()
        loadContentAvailability()
    }

    private fun loadLogs() {
        viewModelScope.launch {
            analyticsRepository.getAllLogs()
                .map { entities ->
                    entities.map { entity ->
                        "[${dateFormat.format(Date(entity.timestamp))}] ${entity.logMessage}"
                    }
                }
                .collect { logList ->
                    _logs.value = logList
                }
        }
    }

    private fun loadContentAvailability() {
        val isAvailable = contentVisibilityManager.isContentAvailable()
        _uiState.value = MenuUiState(isContentAvailable = isAvailable)
    }

    fun toggleContentAvailability(isAvailable: Boolean) {
        contentVisibilityManager.setContentAvailable(isAvailable)
        _uiState.value = MenuUiState(isContentAvailable = isAvailable)
        log("content_available: $isAvailable")
    }

    fun log(message: String) {
        viewModelScope.launch {
            analyticsRepository.log(message)
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            analyticsRepository.clearAllLogs()
        }
    }
}