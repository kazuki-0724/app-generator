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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.zIndex
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import com.waju.factory.app.generator.domain.model.MiniApp
import com.waju.factory.app.generator.ui.theme.darkGray
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    app: MiniApp,
    htmlVirtualPath: String,
    currentPageVersion: Int,
    logs: List<String>,
    showDebug: Boolean,
    currentHtmlContent: String,
    onBack: () -> Unit,
    onToggleDebug: () -> Unit,
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


    BackHandler(enabled = true) {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .zIndex(1f),  // WebView より前面に描画されることを保証
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack, modifier = Modifier.padding(0.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "戻る"
                )
            }
            Text(
                app.title,
                fontSize = 25.sp,
                color = Color.White,
                modifier = Modifier
                    .weight(2f)
                    .padding(horizontal = 8.dp),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge
            )
            IconButton(
                onClick = { showSheet = true },
                modifier = Modifier.weight(0.5f)
            ) {
                Icon(Icons.Default.Settings,
                    "Debug",
                    tint = if (showDebug) darkGray else Color.White
                )
            }
        }

        // WebView は常に Composition に保持し、デバッグパネルを重ねて表示する
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()  // 初回フレームで WebView が領域外に描画するのを防ぐ
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                factory = { context ->
                    createWebView(context, htmlVirtualPath)
                },
                update = { webView ->
                    val targetUrl = buildAppLocalUrl(htmlVirtualPath, currentPageVersion)
                    if (webView.url != targetUrl) {
                        webView.loadUrl(targetUrl)
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
                                logs.takeLast(10).reversed().forEach { log ->
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