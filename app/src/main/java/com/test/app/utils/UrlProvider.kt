package com.test.app.utils

import android.content.Context
import android.util.Base64
import com.test.app.R

object UrlProvider {

    fun getDefaultUrl(context: Context): String {
        return try {
            String(Base64.decode(context.getString(R.string.main), Base64.DEFAULT))
        } catch (e: Exception) {
            "https://fallback.com"
        }
    }

    fun decodeUrl(encoded: String): String {
        return if (encoded.startsWith("base64:")) {
            val base64String = encoded.removePrefix("base64:")
            String(Base64.decode(base64String, Base64.DEFAULT))
        } else {
            encoded
        }
    }
}