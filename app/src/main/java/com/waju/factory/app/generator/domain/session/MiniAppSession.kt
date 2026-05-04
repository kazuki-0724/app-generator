package com.waju.factory.app.generator.domain.session

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.waju.factory.app.generator.domain.model.MiniApp

class MiniAppSession {
    var showDebugPanel by mutableStateOf(false)
        private set

    var currentHtmlContent by mutableStateOf("")
        private set

    var currentHtmlVirtualPath by mutableStateOf("/index.html")
        private set

    var pageLoadVersion by mutableStateOf(0)
        private set

    var importedSiteTreeUri: Uri? by mutableStateOf(null)
        private set

    var debugMessages by mutableStateOf(emptyList<String>())
        private set

    fun openApp(app: MiniApp) {
        currentHtmlContent = app.htmlContent
        currentHtmlVirtualPath = "/${app.title}.html"
        pageLoadVersion += 1
    }

    fun applyImportedApp(app: MiniApp) {
        currentHtmlContent = app.htmlContent
        currentHtmlVirtualPath = "/${app.title}.html"
        pageLoadVersion += 1
    }

    fun setAssetTree(uri: Uri) {
        importedSiteTreeUri = uri
        pageLoadVersion += 1
    }

    fun toggleDebugPanel() {
        showDebugPanel = !showDebugPanel
    }

    fun addDebugLog(logLine: String) {
        debugMessages = (debugMessages + logLine).takeLast(30)
    }
}


