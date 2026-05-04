package com.waju.factory.app.generator.core.logging

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DebugLogger(
    private val tag: String,
    private val onLog: (String) -> Unit
) {
    fun debug(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val logLine = "$timestamp $message"
        Log.d(tag, logLine)
        onLog(logLine)
    }
}

