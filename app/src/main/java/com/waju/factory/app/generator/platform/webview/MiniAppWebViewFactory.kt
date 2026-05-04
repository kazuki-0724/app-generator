package com.waju.factory.app.generator.platform.webview

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.MimeTypeMap
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.documentfile.provider.DocumentFile
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Locale

class MiniAppWebViewFactory(
    private val contentResolver: ContentResolver,
    private val appContext: Context,
    private val appLocalHost: String,
    private val getCurrentHtmlContent: () -> String,
    private val getImportedSiteTreeUri: () -> Uri?,
    private val onDebugLog: (String) -> Unit,
    private val nativeBridgeProvider: () -> Any
) {
    fun createWebView(context: Context, htmlVirtualPath: String): WebView {
        return WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val requestedUri = request?.url ?: return super.shouldInterceptRequest(view, request)
                    if (requestedUri.host != appLocalHost) {
                        return super.shouldInterceptRequest(view, request)
                    }

                    val requestPath = normalizeAppLocalPath(requestedUri.path)
                        ?: return createErrorResponse(400, "Bad Request", "Invalid path")

                    if (requestPath == "/" || requestPath == htmlVirtualPath) {
                        return createHtmlResponse(getCurrentHtmlContent())
                    }

                    serveImportedAsset(requestPath)?.let { return it }

                    onDebugLog("Asset not found: $requestPath")
                    return createErrorResponse(404, "Not Found", "Asset not found: $requestPath")
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    super.onReceivedError(view, request, error)
                    onDebugLog("Load error: ${request?.url} ${error?.errorCode ?: ""}")
                }

                override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    onDebugLog("HTTP ${errorResponse?.statusCode ?: "?"}: ${request?.url}")
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    onDebugLog("Page loaded: ${url ?: "unknown"}")
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    onDebugLog("Console: ${consoleMessage.message()}")
                    return true
                }
            }

            addJavascriptInterface(nativeBridgeProvider(), "AndroidBridge")
        }
    }

    fun buildAppLocalUrl(path: String, version: Int): String {
        return Uri.Builder()
            .scheme("https")
            .authority(appLocalHost)
            .path(path)
            .appendQueryParameter("v", version.toString())
            .build()
            .toString()
    }

    private fun normalizeAppLocalPath(path: String?): String? {
        val rawPath = path?.ifBlank { "/" } ?: "/"
        val normalizedSegments = rawPath
            .split('/')
            .filter { it.isNotBlank() }

        if (normalizedSegments.any { it == "." || it == ".." }) {
            return null
        }

        return if (normalizedSegments.isEmpty()) "/" else "/${normalizedSegments.joinToString("/")}"
    }

    private fun createHtmlResponse(html: String): WebResourceResponse {
        return createSuccessResponse(
            mimeType = "text/html",
            encoding = "UTF-8",
            inputStream = ByteArrayInputStream(html.toByteArray(Charsets.UTF_8))
        )
    }

    private fun serveImportedAsset(requestPath: String): WebResourceResponse? {
        val treeUri = getImportedSiteTreeUri() ?: return null
        val rootDirectory = DocumentFile.fromTreeUri(appContext, treeUri) ?: return null
        val relativePath = requestPath.removePrefix("/")
        val assetFile = findFileByRelativePath(rootDirectory, relativePath) ?: return null
        if (!assetFile.isFile) {
            return null
        }

        val mimeType = resolveMimeType(assetFile)
        val encoding = if (isTextMimeType(mimeType)) "UTF-8" else null
        val inputStream = contentResolver.openInputStream(assetFile.uri) ?: return null
        return createSuccessResponse(mimeType, encoding, inputStream)
    }

    private fun createSuccessResponse(
        mimeType: String,
        encoding: String?,
        inputStream: InputStream
    ): WebResourceResponse {
        return WebResourceResponse(
            mimeType,
            encoding,
            200,
            "OK",
            defaultResponseHeaders(),
            inputStream
        )
    }

    private fun createErrorResponse(
        statusCode: Int,
        reasonPhrase: String,
        body: String
    ): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            statusCode,
            reasonPhrase,
            defaultResponseHeaders(),
            ByteArrayInputStream(body.toByteArray(Charsets.UTF_8))
        )
    }

    private fun defaultResponseHeaders(): Map<String, String> {
        return mapOf(
            "Access-Control-Allow-Origin" to "*",
            "Cache-Control" to "no-cache, no-store, must-revalidate"
        )
    }

    private fun findFileByRelativePath(rootDirectory: DocumentFile, relativePath: String): DocumentFile? {
        val segments = relativePath
            .split('/')
            .filter { it.isNotBlank() }

        var current: DocumentFile = rootDirectory
        for (segment in segments) {
            if (segment == "." || segment == "..") {
                return null
            }
            current = current.findFile(segment) ?: return null
        }
        return current
    }

    private fun resolveMimeType(file: DocumentFile): String {
        contentResolver.getType(file.uri)?.let { return it }

        val extension = file.name
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase(Locale.US)
            .orEmpty()

        return when (extension) {
            "css" -> "text/css"
            "js", "mjs" -> "application/javascript"
            "json" -> "application/json"
            "svg" -> "image/svg+xml"
            "html", "htm" -> "text/html"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
        }
    }

    private fun isTextMimeType(mimeType: String): Boolean {
        return mimeType.startsWith("text/") ||
            mimeType.contains("javascript") ||
            mimeType.contains("json") ||
            mimeType.contains("xml") ||
            mimeType == "image/svg+xml"
    }
}


