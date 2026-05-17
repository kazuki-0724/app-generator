package com.waju.factory.app.generator

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.webkit.ValueCallback
import androidx.compose.runtime.Composable
import com.waju.factory.app.generator.core.logging.DebugLogger
import com.waju.factory.app.generator.domain.session.MiniAppSession
import com.waju.factory.app.generator.platform.bridge.NativeBridge
import com.waju.factory.app.generator.platform.document.DocumentPickerHelper
import com.waju.factory.app.generator.platform.webview.MiniAppWebViewFactory
import com.waju.factory.app.generator.ui.ViewerScreen
import com.waju.factory.app.generator.ui.GridViewScreen
import com.waju.factory.app.generator.ui.ImportAppDialog
import com.waju.factory.app.generator.ui.theme.AppGeneratorTheme
import com.waju.factory.app.generator.viewModel.MainViewModel
import androidx.core.net.toUri
import androidx.navigation.compose.NavHost

@SuppressLint("SetJavaScriptEnabled")
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var sharedPreferences: SharedPreferences
    private val appLocalHost = "app.local"
    private val session = MiniAppSession()
    private val debugLogger = DebugLogger("MiniAppWebView") { session.addDebugLog(it) }
    private val documentPickerHelper by lazy { DocumentPickerHelper(contentResolver) }
    private var webViewFilePathCallback: ValueCallback<Array<Uri>>? = null

    // 1. ファイル選択結果を受け取るランチャーの登録
    private val fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val dataString = result.data?.dataString
            val clipData = result.data?.clipData

            var results: Array<Uri>? = null

            if (dataString != null) {
                results = arrayOf(dataString.toUri())
            } else if (clipData != null) {
                results = Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
            }

            webViewFilePathCallback?.onReceiveValue(results)
        } else {
            // 【重要】キャンセルされた場合は必ず null を返して WebView のブロックを解除する
            webViewFilePathCallback?.onReceiveValue(null)
        }
        // 使用後は必ず null にリセットする
        webViewFilePathCallback = null
    }

    private val webViewFactory by lazy {
        MiniAppWebViewFactory(
            contentResolver = contentResolver,
            appContext = this,
            appLocalHost = appLocalHost,
            getCurrentHtmlContent = { session.currentHtmlContent },
            getImportedSiteTreeUri = { session.importedSiteTreeUri },
            onDebugLog = debugLogger::debug,
            nativeBridgeProvider = {
                NativeBridge(sharedPreferences) { message ->
                    runOnUiThread {
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onShowFileChooserDelegate = { callback, params ->
                // 前回のコールバックが残っていればキャンセル
                webViewFilePathCallback?.onReceiveValue(null)
                webViewFilePathCallback = callback

                val intent = params.createIntent()
                try {
                    fileChooserLauncher.launch(intent)
                    true // 処理をハンドリングしたことを返す
                } catch (e: Exception) {
                    e.printStackTrace()
                    webViewFilePathCallback = null
                    Toast.makeText(this, "対応するアプリが見つかりません", Toast.LENGTH_SHORT).show()
                    false
                }
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        sharedPreferences = getSharedPreferences("MiniAppData", MODE_PRIVATE)
        WebView.setWebContentsDebuggingEnabled(true)

        setContent {
            AppGeneratorTheme {
                MainScreen()
            }
        }
    }

    @Composable
    fun MainScreen() {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            val apps by viewModel.miniApps.collectAsState()
            val selectedAppId by viewModel.selectedAppId.collectAsState()
            val showNewAppDialog by viewModel.showNewAppDialog.collectAsState()
            val newAppTitle by viewModel.newAppTitle.collectAsState()
            val htmlContent by viewModel.htmlContent.collectAsState()

            if (selectedAppId == null) {
                GridViewScreen(
                    apps = apps,
                    onAppClick = { app ->
                        viewModel.openApp(app.id)
                        session.openApp(app)
                    },
                    onAppDelete = { app -> viewModel.deleteApp(app) },
                    onAddNew = { viewModel.showDialog() },
                    onAppDetailDataDelete = { app ->
                        viewModel.deleteAppDetailData(app.id)
                    }
                )
            } else {
                val app = apps.find { it.id == selectedAppId }
                if (app != null) {
                    ViewerScreen(
                        app = app,
                        htmlVirtualPath = session.currentHtmlVirtualPath,
                        currentPageVersion = session.pageLoadVersion,
                        logs = session.debugMessages,
                        currentHtmlContent = session.currentHtmlContent,
                        onBack = { viewModel.closeEditor() },
                        onSelectAssetFolder = { triggerSelectAssetFolder() },
                        onSave = { updated -> viewModel.updateApp(updated) },
                        createWebView = webViewFactory::createWebView,
                        buildAppLocalUrl = webViewFactory::buildAppLocalUrl
                    )
                }
            }

            if (showNewAppDialog) {
                ImportAppDialog(
                    onDismissRequest = { viewModel.dismissDialog() },
                    title = newAppTitle,
                    onTitleChange = { viewModel.updateNewAppTitle(it) },
                    htmlContent = htmlContent,
                    onHtmlContentChange = {viewModel.updateHtmlContent(it)},
                    onImport = { triggerImport() },
                    onImportFromCanvas = { text ->
                        val newApp = viewModel.importFromCanvas(text)
                        viewModel.openApp(newApp.id)
                        session.openApp(newApp)
                    }
                )
            }
        }
    }

    private fun takeReadPermission(uri: Uri, flags: Int) {
        documentPickerHelper.takeReadPermission(uri, flags) { deniedUri ->
            debugLogger.debug("Persistable permission unavailable: $deniedUri")
        }
    }

    private fun triggerImport() {
        importLauncher.launch(documentPickerHelper.buildHtmlImportIntent())
    }

    private fun triggerSelectAssetFolder(initialUri: Uri? = null) {
        assetFolderLauncher.launch(documentPickerHelper.buildAssetFolderIntent(initialUri))
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        handleImportResult(result)
    }

    private val assetFolderLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        handleAssetFolderResult(result)
    }

    private fun handleImportResult(result: ActivityResult) {
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    takeReadPermission(uri, result.data?.flags ?: 0)
                    val htmlString = documentPickerHelper.readText(uri)

                    val newApp = viewModel.addApp(htmlString)
                    viewModel.openApp(newApp.id)
                    viewModel.dismissDialog()
                    session.applyImportedApp(newApp)
                    debugLogger.debug("Imported: ${newApp.title}")
                    Toast.makeText(this, "インポート完了", Toast.LENGTH_SHORT).show()
                    triggerSelectAssetFolder(uri)
                } catch (e: Exception) {
                    e.printStackTrace()
                    debugLogger.debug("Import failed: ${e.message}")
                    Toast.makeText(this, "読み込み失敗", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleAssetFolderResult(result: ActivityResult) {
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    takeReadPermission(uri, result.data?.flags ?: 0)
                    session.setAssetTree(uri)
                    debugLogger.debug("Asset folder selected")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            debugLogger.debug("Asset folder selection skipped")
        }
    }
}