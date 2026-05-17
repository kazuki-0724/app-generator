package com.waju.factory.app.generator.feature.viewer

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.waju.factory.app.generator.core.webview.MiniAppWebViewFactory
import com.waju.factory.app.generator.data.model.MiniApp
import com.waju.factory.app.generator.data.session.MiniAppSession
import com.waju.factory.app.generator.ui.ViewerContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    app: MiniApp,
    htmlVirtualPath: String,
    currentPageVersion: Int,
    logs: List<String>,
    currentHtmlContent: String,
    onBack: () -> Unit,
    webViewFactory: MiniAppWebViewFactory
) {
    ViewerContent(
        app = app,
        htmlVirtualPath = htmlVirtualPath,
        currentPageVersion = currentPageVersion,
        logs = logs,
        currentHtmlContent = currentHtmlContent,
        onBack = onBack,
        createWebView = { context, path -> webViewFactory.createWebView(context, path) },
        buildAppLocalUrl = { path, version -> webViewFactory.buildAppLocalUrl(path, version) }
    )
}