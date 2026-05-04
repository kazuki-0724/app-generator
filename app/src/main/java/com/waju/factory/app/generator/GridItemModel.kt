package com.waju.factory.app.generator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// --- データモデル ---
data class GridItemModel(val id: Int, val title: String)

// --- グリッド画面 ---
@Composable
fun GridScreen(
    items: List<GridItemModel>,
    onItemClick: (Int) -> Unit // NavControllerの代わりにラムダを受け取る
) {
    // 2列のグリッド
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items, key = { it.id }) { item ->
            GridItemCard(
                item = item,
                onClick = { onItemClick(item.id) }
            )
        }
    }
}

// --- グリッドの個別アイテム ---
@Composable
fun GridItemCard(
    item: GridItemModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // 正方形にする
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = item.title, style = MaterialTheme.typography.titleMedium)
        }
    }
}

// --- 遷移先の詳細画面 ---
@Composable
fun DetailScreen(
    itemId: Int,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "詳細画面: アイテムID = $itemId", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onNavigateBack) {
            Text("戻る")
        }
    }
}