package com.test.app.managers

import android.content.Context
import com.test.app.utils.EncryptSharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentVisibilityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val KEY_CONTENT_AVAILABLE = "content_available"
    }

    fun isContentAvailable(): Boolean {
        return EncryptSharedPreferences.getBoolean(context, KEY_CONTENT_AVAILABLE, true)
    }

    fun setContentAvailable(isAvailable: Boolean) {
        EncryptSharedPreferences.putBoolean(context, KEY_CONTENT_AVAILABLE, isAvailable)
    }
}