package com.waju.factory.app.generator.platform.webview

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.ConsoleMessage
import android.webkit.MimeTypeMap
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.ViewGroup
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
    private val nativeBridgeProvider: () -> Any,
    private val onShowFileChooserDelegate: ((
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: WebChromeClient.FileChooserParams
    ) -> Boolean)? = null
) {
    @SuppressLint("JavascriptInterface")
    fun createWebView(context: Context, htmlVirtualPath: String): WebView {
        return WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            // viewport meta タグを有効化（レイアウト崩れ防止に必須）
            settings.useWideViewPort = true
            // overview による初期縮小は高さ計算と競合しやすいため無効化する
            settings.loadWithOverviewMode = false

            // システムのフォントサイズ設定をWebViewに反映させない
            settings.textZoom = 100

            // 動画・音声のインライン再生と自動再生を許可
            settings.mediaPlaybackRequiresUserGesture = false

            // Chrome DevTools でのリモートデバッグを有効化（デバッグビルドのみ）
            WebView.setWebContentsDebuggingEnabled(
                appContext.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
            )

            webViewClient = object : WebViewClient() {
                private var lastRelayoutUrl: String? = null

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

                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    onDebugLog("Page start: ${url ?: "unknown"} view=${view?.width ?: 0}x${view?.height ?: 0}")
                }

                override fun onPageCommitVisible(view: WebView?, url: String?) {
                    super.onPageCommitVisible(view, url)
                    onDebugLog("First paint committed: ${url ?: "unknown"} view=${view?.width ?: 0}x${view?.height ?: 0}")
                    dispatchRelayout(view)

                    if (view != null) {
                        view.postVisualStateCallback(System.nanoTime(), object : WebView.VisualStateCallback() {
                            override fun onComplete(requestId: Long) {
                            onDebugLog("Visual state ready: ${url ?: "unknown"}")
                            dispatchRelayout(view)
                            }
                        })
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    onDebugLog("Page loaded: ${url ?: "unknown"}")
                    // First paint 前後で保険として再計算を1回だけ行う
                    if (url != null && url != lastRelayoutUrl) {
                        dispatchRelayout(view)
                        lastRelayoutUrl = url
                    }
                }

                private fun dispatchRelayout(view: WebView?) {
                    val script = """
                        (function() {
                            window.dispatchEvent(new Event('resize'));
                            window.requestAnimationFrame(function() {
                                window.dispatchEvent(new Event('resize'));
                            });
                        })();
                    """.trimIndent()

                    view?.evaluateJavascript(script, null)
                    view?.postDelayed({ view.evaluateJavascript("window.dispatchEvent(new Event('resize'));", null) }, 80L)
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    val message = consoleMessage.message()
                    // "zero dimensions" は Chromium レンダラーが初期フレームでサーフェスを
                    // まだ確立できていないときに出す内部警告。実害はないのでフィルタリングする。
                    if (message.contains("zero dimensions", ignoreCase = true)) {
                        return true
                    }
                    onDebugLog("Console: $message")
                    return true
                }

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    // デリゲートが設定されていれば Activity 側に処理を任せる
                    if (filePathCallback != null && fileChooserParams != null && onShowFileChooserDelegate != null) {
                        return onShowFileChooserDelegate.invoke(filePathCallback, fileChooserParams)
                    }
                    return super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
                }

                override fun onPermissionRequest(request: PermissionRequest?) {
                    // 要求されたリソースの中に「カメラ（VIDEO_CAPTURE）」が含まれているかチェック
                    val hasCameraRequest = request?.resources?.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE) == true

                    if (hasCameraRequest) {
                        // 本来はここでAndroidネイティブ側のCAMERA権限が許可されているか(checkSelfPermission)を
                        // 確認するのが安全ですが、手っ取り早くWebViewに許可を出す場合は以下を実行します。
                        request?.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                    } else {
                        super.onPermissionRequest(request)
                    }
                }

                // 【追加】権限要求がキャンセルされたときの処理（念のため）
                override fun onPermissionRequestCanceled(request: PermissionRequest?) {
                    super.onPermissionRequestCanceled(request)
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


