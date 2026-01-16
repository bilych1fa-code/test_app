package com.test.app.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object EncryptSharedPreferences {
    private const val PREFS_NAME = "secure_app_prefs"

    @Volatile
    private var encryptedPrefs: SharedPreferences? = null

    private fun getPrefs(context: Context): SharedPreferences {
        return encryptedPrefs ?: synchronized(this) {
            encryptedPrefs ?: createEncryptedPreferences(context).also {
                encryptedPrefs = it
            }
        }
    }

    private fun createEncryptedPreferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun putString(context: Context, key: String, value: String) {
        getPrefs(context).edit().putString(key, value).apply()
    }

    fun getString(context: Context, key: String, defaultValue: String? = null): String? {
        return getPrefs(context).getString(key, defaultValue)
    }

    fun putBoolean(context: Context, key: String, value: Boolean) {
        getPrefs(context).edit().putBoolean(key, value).apply()
    }

    fun getBoolean(context: Context, key: String, defaultValue: Boolean = false): Boolean {
        return getPrefs(context).getBoolean(key, defaultValue)
    }
    fun putLong(context: Context, key: String, value: Long){
        getPrefs(context).edit().putLong(key,value).apply()
    }
    fun getLong(context: Context, key: String, defaultValue: Long = 0L) : Long{
        return getPrefs(context).getLong(key, defaultValue)
    }
    fun putInt(context: Context, key: String, value: Int){
        getPrefs(context).edit().putInt(key,value).apply()
    }
    fun getInt(context: Context, key: String, defaultValue: Int = 0) : Int{
        return getPrefs(context).getInt(key, defaultValue)
    }


}