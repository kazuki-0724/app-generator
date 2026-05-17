package com.waju.factory.app.generator.feature.home

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.content.ClipboardManager
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waju.factory.app.generator.data.model.MiniApp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

@Composable
fun HomeContent(
    apps: List<MiniApp>,
    onAppClick: (MiniApp) -> Unit,
    onAppDelete: (MiniApp) -> Unit,
    onAddNew: () -> Unit,
    onAppDetailDataDelete: (MiniApp) -> Unit,
    goSettings: () -> Unit,
    onDismissRequest: () -> Unit,
    onTitleChange: (String) -> Unit,
    onHtmlContentChange: (String) -> Unit,
    showNewAppDialog: Boolean,
    newAppTitle: String,
    htmlContent: String,
) {
    var appToDelete by remember { mutableStateOf<MiniApp?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box {
                // ヘッダーエリア
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "App Gallery",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(20.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = goSettings,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            }

            // メインエリア
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(apps) { app ->
                    MiniAppCardComposable(
                        app = app,
                        onClick = { onAppClick(app) },
                        onLongClick = { appToDelete = app }
                    )
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = onAddNew,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(Icons.Default.Add, "Add")
        }

        if (appToDelete != null) {
            AppDialog(
                title = appToDelete!!.title,
                onDelete = {
                    onAppDelete(appToDelete!!)
                    appToDelete = null
                },
                onDismiss = { appToDelete = null },
                onAppDetailDataDelete = {
                    onAppDetailDataDelete(appToDelete!!)
                    appToDelete = null
                }
            )
        }

        if (showNewAppDialog) {
            ImportAppDialog(
                onDismissRequest = onDismissRequest,
                title = newAppTitle,
                onTitleChange = onTitleChange,
                htmlContent = htmlContent,
                onHtmlContentChange = onHtmlContentChange,
                onImport = {},
                onImportFromCanvas = {}
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MiniAppCardComposable(
    app: MiniApp,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        shadowElevation = 2.dp
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFFEEF2FF), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.title.take(1).uppercase(LocalLocale.current.platformLocale),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }

                Text(
                    text = app.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = formatTimestamp(app.timestamp),
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppDialog(
    title: String,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    onAppDetailDataDelete: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // タイトル
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = "close dialog",
                            Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(
                        onClick = onDelete
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "import")
                        Text("アプリの削除")
                    }

                    TextButton(
                        onClick = onAppDetailDataDelete
                    ) {
                        Icon(Icons.Default.Code, contentDescription = "write code")
                        Text("アプリのデータ削除")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportAppDialog(
    onDismissRequest: () -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    htmlContent: String,
    onHtmlContentChange: (String) -> Unit,
    onImport: () -> Unit,
    onImportFromCanvas: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    BasicAlertDialog(
        onDismissRequest = onDismissRequest
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // タイトル
                Text(
                    text = "新規ミニアプリ",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("タイトル") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onImport() })
                )

                OutlinedTextField(
                    value = htmlContent,
                    onValueChange = onHtmlContentChange,
                    label = { Text("HTMLコード") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    singleLine = false
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween // 左右に振り分け
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Cancel, contentDescription = "close dialog")
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp) // ボタン間の余白
                    ) {
                        TextButton(
                            onClick = onImport,
                            enabled = title.isNotBlank() && htmlContent.isBlank()
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = "import")
                        }

                        TextButton(
                            onClick = {
                                if (clipboardManager.hasPrimaryClip()) {
                                    val clipData = clipboardManager.primaryClip
                                    if (clipData != null && clipData.itemCount > 0) {
                                        val text = clipData.getItemAt(0).text
                                        if (text != null) {
                                            onImportFromCanvas(text.toString())
                                        }
                                    }
                                }
                            },
                            enabled = title.isNotBlank() && htmlContent.isBlank()
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "direct paste")
                        }

                        TextButton(
                            onClick = { onImportFromCanvas("") },
                            enabled = title.isNotBlank() && htmlContent.isNotBlank()
                        ) {
                            Icon(Icons.Default.Code, contentDescription = "write code")
                        }
                    }
                }
            }
        }
    }
}