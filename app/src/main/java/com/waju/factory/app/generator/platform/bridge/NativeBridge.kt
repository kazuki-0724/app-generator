package com.waju.factory.app.generator.platform.bridge

import android.content.SharedPreferences
import android.webkit.JavascriptInterface

class NativeBridge(
    private val sharedPreferences: SharedPreferences,
    private val onShowToast: (String) -> Unit
) {
    @JavascriptInterface
    fun saveData(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    @JavascriptInterface
    fun loadData(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    @JavascriptInterface
    fun showToast(message: String) {
        onShowToast(message)
    }
}


