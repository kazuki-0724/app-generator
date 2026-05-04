package com.waju.factory.app.generator.ui

import android.content.Context
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.waju.factory.app.generator.domain.model.MiniApp
import com.waju.factory.app.generator.ui.theme.darkNavy
import com.waju.factory.app.generator.ui.theme.lightGray
import com.waju.factory.app.generator.ui.theme.orange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GridViewScreen(
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
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "App Library",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier
                    .padding(20.dp)
            )

            // 3. コンテンツ部分
            if (apps.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(), // 残りのスペースを埋める
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ミニアプリがまだありません", color = Color.White)
                    Text("＋ボタンで新規作成してください", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                // weight(1f) を指定して、タイトル以外の残りの画面いっぱいに広げる
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
                            onDelete = { onAppDelete(app) }
                        )
                    }
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
            .background(lightGray, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(6.dp)
            .border(3.dp, orange, RoundedCornerShape(8.dp))
            .padding(6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                app.title,
                fontSize = 20.sp,
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).format(Date(app.timestamp)),
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
fun NewAppDialogScreen(
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
