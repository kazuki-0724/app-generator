package com.waju.factory.app.generator

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.documentfile.provider.DocumentFile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class MiniApp(
    val id: String,
    val title: String,
    val htmlContent: String,
    val timestamp: Long
)

@SuppressLint("SetJavaScriptEnabled")
class MainActivity : ComponentActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()
    private val appLocalHost = "app.local"
    private val webViewLogTag = "MiniAppWebView"

    // UI State
    private var miniApps by mutableStateOf(emptyList<MiniApp>())
    private var selectedAppId by mutableStateOf<String?>(null)
    private var showNewAppDialog by mutableStateOf(false)
    private var newAppTitle by mutableStateOf("")
    private var showDebugPanel by mutableStateOf(false)

    // WebView State
    private var currentHtmlContent by mutableStateOf("")
    private var currentHtmlVirtualPath by mutableStateOf("/index.html")
    private var pageLoadVersion by mutableStateOf(0)
    private var importedSiteTreeUri: Uri? = null
    private var debugMessages by mutableStateOf(emptyList<String>())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("MiniAppData", MODE_PRIVATE)
        WebView.setWebContentsDebuggingEnabled(true)
        loadMiniApps()

        setContent {
            if (selectedAppId == null) {
                GridViewScreen(
                    apps = miniApps,
                    onAppClick = { app -> openApp(app) },
                    onAppDelete = { app -> deleteApp(app) },
                    onAddNew = { showNewAppDialog = true }
                )
            } else {
                val app = miniApps.find { it.id == selectedAppId }
                if (app != null) {
                    EditorViewScreen(
                        app = app,
                        onBack = { closeEditor() },
                        onSave = { updated -> saveApp(updated) }
                    )
                }
            }

            if (showNewAppDialog) {
                NewAppDialogScreen(
                    title = newAppTitle,
                    onTitleChange = { newAppTitle = it },
                    onImport = { triggerImport() },
                    onDismiss = { dismissDialog() }
                )
            }
        }
    }

    @Composable
    private fun GridViewScreen(
        apps: List<MiniApp>,
        onAppClick: (MiniApp) -> Unit,
        onAppDelete: (MiniApp) -> Unit,
        onAddNew: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            if (apps.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ミニアプリがまだありません")
                    Text("＋ボタンで新規作成してください", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(apps) { app ->
                        MiniAppCardComposable(
                            app = app,
                            onClick = { onAppClick(app) },
                            onDelete = { onAppDelete(app) }
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = onAddNew,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Add")
            }
        }
    }

    @Composable
    private fun MiniAppCardComposable(
        app: MiniApp,
        onClick: () -> Unit,
        onDelete: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    app.title,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(app.timestamp)),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp),
            ) {
                Icon(Icons.Default.Close, "Delete", modifier = Modifier.size(16.dp))
            }
        }
    }

    @Composable
    private fun NewAppDialogScreen(
        title: String,
        onTitleChange: (String) -> Unit,
        onImport: () -> Unit,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("新規ミニアプリ") },
            text = {
                TextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("タイトル") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onImport() })
                )
            },
            confirmButton = {
                Button(
                    onClick = { if (title.isNotBlank()) onImport() },
                    enabled = title.isNotBlank()
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    @Composable
    private fun EditorViewScreen(
        app: MiniApp,
        onBack: () -> Unit,
        onSave: (MiniApp) -> Unit
    ) {
        val htmlVirtualPath = currentHtmlVirtualPath
        val currentPageVersion = pageLoadVersion
        val logs = debugMessages
        val showDebug = showDebugPanel

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Text("< Back")
                }
                Text(
                    app.title,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .weight(2f)
                        .padding(horizontal = 8.dp),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                IconButton(
                    onClick = { showDebugPanel = !showDebugPanel },
                    modifier = Modifier.weight(0.5f)
                ) {
                    Icon(Icons.Default.Settings, "Debug")
                }
            }

            if (showDebug) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                        .padding(horizontal = 8.dp)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp))
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("WebView Debug", fontSize = 12.sp)
                    if (logs.isEmpty()) {
                        Text(
                            "読み込みログと JavaScript コンソールがここに表示されます。",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    } else {
                        logs.takeLast(10).reversed().forEach { log ->
                            Text(text = log, fontSize = 10.sp, color = Color.DarkGray)
                        }
                    }
                }
            }

            AndroidView(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                factory = { context ->
                    createWebView(context, htmlVirtualPath, currentPageVersion)
                },
                update = { webView ->
                    val targetUrl = buildAppLocalUrl(htmlVirtualPath, currentPageVersion)
                    if (webView.url != targetUrl) {
                        webView.loadUrl(targetUrl)
                    }
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { triggerSelectAssetFolder() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Asset Folder")
                }
                Button(
                    onClick = {
                        val updated = app.copy(
                            htmlContent = currentHtmlContent,
                            timestamp = System.currentTimeMillis()
                        )
                        onSave(updated)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }
        }
    }

    private fun createWebView(context: Context, htmlVirtualPath: String, currentPageVersion: Int): WebView {
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
                        return createHtmlResponse(currentHtmlContent)
                    }

                    serveImportedAsset(requestPath)?.let { return it }

                    appendDebugLog("Asset not found: $requestPath")
                    return createErrorResponse(404, "Not Found", "Asset not found: $requestPath")
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    super.onReceivedError(view, request, error)
                    appendDebugLog("Load error: ${request?.url} ${error?.errorCode ?: ""}")
                }

                override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    appendDebugLog("HTTP ${errorResponse?.statusCode ?: "?"}: ${request?.url}")
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    appendDebugLog("Page loaded: ${url ?: "unknown"}")
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    appendDebugLog("Console: ${consoleMessage.message()}")
                    return true
                }
            }

            addJavascriptInterface(NativeBridge(), "AndroidBridge")
        }
    }

    private fun openApp(app: MiniApp) {
        selectedAppId = app.id
        currentHtmlContent = app.htmlContent
        currentHtmlVirtualPath = "/${app.title}.html"
        pageLoadVersion += 1
        clearDebugLog()
        showDebugPanel = false
        importedSiteTreeUri = null
    }

    private fun closeEditor() {
        selectedAppId = null
        showDebugPanel = false
    }

    private fun deleteApp(app: MiniApp) {
        miniApps = miniApps.filter { it.id != app.id }
        saveMiniApps(miniApps)
        Toast.makeText(this, "削除しました", Toast.LENGTH_SHORT).show()
    }

    private fun saveApp(updated: MiniApp) {
        miniApps = miniApps.map { if (it.id == updated.id) updated else it }
        saveMiniApps(miniApps)
        currentHtmlContent = updated.htmlContent
        pageLoadVersion += 1
        Toast.makeText(this, "保存しました", Toast.LENGTH_SHORT).show()
    }

    private fun dismissDialog() {
        showNewAppDialog = false
        newAppTitle = ""
    }

    private fun buildAppLocalUrl(path: String, version: Int): String {
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
        val treeUri = importedSiteTreeUri ?: return null
        val rootDirectory = DocumentFile.fromTreeUri(this, treeUri) ?: return null
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

    private fun appendDebugLog(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val logLine = "$timestamp $message"
        Log.d(webViewLogTag, logLine)
        runOnUiThread {
            debugMessages = (debugMessages + logLine).takeLast(30)
        }
    }

    private fun clearDebugLog() {
        debugMessages = emptyList()
    }

    private fun resolveDisplayName(uri: Uri): String? {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (columnIndex >= 0) {
                    return cursor.getString(columnIndex)
                }
            }
        }
        return DocumentFile.fromSingleUri(this, uri)?.name
    }

    private fun takeReadPermission(uri: Uri, flags: Int) {
        val persistableFlags = flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
        val flagsToPersist = if (persistableFlags != 0) persistableFlags else Intent.FLAG_GRANT_READ_URI_PERMISSION
        try {
            contentResolver.takePersistableUriPermission(uri, flagsToPersist)
        } catch (_: SecurityException) {
            appendDebugLog("Persistable permission unavailable: $uri")
        }
    }

    private fun triggerImport() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/html"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        importLauncher.launch(intent)
    }

    private fun triggerSelectAssetFolder(initialUri: Uri? = null) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            initialUri?.let { putExtra(DocumentsContract.EXTRA_INITIAL_URI, it) }
        }
        assetFolderLauncher.launch(intent)
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    takeReadPermission(uri, result.data?.flags ?: 0)
                    val htmlString = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: throw IllegalStateException("Could not read HTML")
                    
                    val appId = UUID.randomUUID().toString()
                    val title = newAppTitle.ifBlank { "Unnamed App" }
                    val newApp = MiniApp(appId, title, htmlString, System.currentTimeMillis())
                    
                    miniApps = miniApps + newApp
                    saveMiniApps(miniApps)
                    openApp(newApp)
                    dismissDialog()
                    appendDebugLog("Imported: $title")
                    Toast.makeText(this, "インポート完了", Toast.LENGTH_SHORT).show()
                    triggerSelectAssetFolder(uri)
                } catch (e: Exception) {
                    e.printStackTrace()
                    appendDebugLog("Import failed: ${e.message}")
                    Toast.makeText(this, "読み込み失敗", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val assetFolderLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    takeReadPermission(uri, result.data?.flags ?: 0)
                    importedSiteTreeUri = uri
                    pageLoadVersion += 1
                    appendDebugLog("Asset folder selected")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            appendDebugLog("Asset folder selection skipped")
        }
    }

    private fun loadMiniApps() {
        try {
            val json = sharedPreferences.getString("mini_apps_list", "[]") ?: "[]"
            miniApps = gson.fromJson(json, object : TypeToken<List<MiniApp>>() {}.type)
        } catch (e: Exception) {
            e.printStackTrace()
            miniApps = emptyList()
        }
    }

    private fun saveMiniApps(apps: List<MiniApp>) {
        try {
            val json = gson.toJson(apps)
            sharedPreferences.edit().putString("mini_apps_list", json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    inner class NativeBridge {
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
            runOnUiThread {
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
