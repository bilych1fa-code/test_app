package com.test.app.data

data class DeepLinkData(
    val url: String,
    val title: String? = null,
    val isDeepLink: Boolean = false
)