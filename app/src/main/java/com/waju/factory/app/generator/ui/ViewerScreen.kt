package com.waju.factory.app.generator.ui

import android.content.Context
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.zIndex
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import com.waju.factory.app.generator.domain.model.MiniApp
import com.waju.factory.app.generator.ui.theme.DarkGray
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    app: MiniApp,
    htmlVirtualPath: String,
    currentPageVersion: Int,
    logs: List<String>,
    currentHtmlContent: String,
    onBack: () -> Unit,
    onSelectAssetFolder: () -> Unit,
    onSave: (MiniApp) -> Unit,
    createWebView: (Context, String) -> WebView,
    buildAppLocalUrl: (String, Int) -> String
) {
    // 1. シートを表示するかどうかのフラグ
    var showSheet by remember { mutableStateOf(false) }
    // 2. シートの状態管理（アニメーションなどを制御）
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var fullScreen by remember { mutableStateOf(false) }
    // WebView コンテナのレイアウトサイズが確定したかどうか
    var webViewLayoutReady by remember { mutableStateOf(false) }

    // ロード対象が変わったら、レイアウト準備状態を必ず再判定する
    LaunchedEffect(htmlVirtualPath, currentPageVersion, fullScreen) {
        webViewLayoutReady = false
    }


    BackHandler(enabled = true) {
        if (fullScreen) {
            fullScreen = false
            return@BackHandler
        }
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        if (!fullScreen) {
            Column(modifier = Modifier.fillMaxWidth().zIndex(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                    Text(
                        app.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier
                            .weight(2f)
                            .padding(horizontal = 8.dp),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                    IconButton(
                        onClick = { fullScreen = !fullScreen },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow,
                            "FullScreen",
                            tint = DarkGray,
                        )
                    }
                    IconButton(
                        onClick = { showSheet = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Info,
                            "Debug",
                            tint = DarkGray
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.16f), Color.Transparent)
                            )
                        )
                )
            }
        }

        // WebView は常に Composition に保持し、デバッグパネルを重ねて表示する
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                factory = { context ->
                    createWebView(context, htmlVirtualPath).also { webView ->
                        // View.onLayout() 完了後、さらに post で1ループ遅延させてから loadUrl する。
                        // こうすることで Chromium のレンダラーがサーフェスサイズを確定した後に
                        // ページロードが始まり、"Skipped zero dimensions" を抑制できる。
                        var initialLoadTriggered = false
                        webView.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
                            val width = right - left
                            val height = bottom - top
                            if (!initialLoadTriggered && width > 0 && height > 0) {
                                initialLoadTriggered = true
                                webView.post {
                                    webViewLayoutReady = true
                                }
                            }
                        }
                    }
                },
                update = { webView ->
                    // WebView の View.onLayout() 完了前はロードしない
                    val hasMeasuredSize = webView.isLaidOut && webView.width > 0 && webView.height > 0
                    if (!webViewLayoutReady && hasMeasuredSize) {
                        webViewLayoutReady = true
                    }

                    if (webViewLayoutReady && hasMeasuredSize) {
                        val targetUrl = buildAppLocalUrl(htmlVirtualPath, currentPageVersion)
                        if (webView.url != targetUrl) {
                            webView.loadUrl(targetUrl)
                        }
                    }
                }
            )

            if (showSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        // 閉じるとき
                        scope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showSheet = false
                            }
                        } }, // シートの外側をタップした時
                    sheetState = sheetState
                ) {
                    Column {
                        Row (
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Button(
                                onClick = onSelectAssetFolder,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Asset Folder")
                            }

                            Spacer(modifier = Modifier.width(6.dp))

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

                        Column (
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp)
                                .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                                .padding(3.dp)
                                .verticalScroll(rememberScrollState())
                        ){
                            Text("WebView Debug", fontSize = 12.sp, color = Color.White)
                            if (logs.isEmpty()) {
                                Text(
                                    "読み込みログと JavaScript コンソールがここに表示されます。",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            } else {
                                logs.takeLast(100).reversed().forEach { log ->
                                    Text(text = log, fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}