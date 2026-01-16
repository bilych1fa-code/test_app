package com.test.app.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class OpenWebViewViewModel : ViewModel() {

    private val _menuActions = MutableSharedFlow<MenuAction>()
    val menuActions: SharedFlow<MenuAction> = _menuActions.asSharedFlow()

    suspend fun onRefreshClicked() {
        Log.d(TAG, "COD_ onRefreshClicked called")
        _menuActions.emit(MenuAction.Refresh)
        Log.d(TAG, "COD_ Refresh action emitted")
    }

    suspend fun onOpenInBrowserClicked() {
        Log.d(TAG, "COD_ onOpenInBrowserClicked called")
        _menuActions.emit(MenuAction.OpenInBrowser)
        Log.d(TAG, "COD_ OpenInBrowser action emitted")
    }

    companion object {
        private const val TAG = "COD_OpenWebViewVM"
    }
}

sealed class MenuAction {
    object Refresh : MenuAction()
    object OpenInBrowser : MenuAction()
}