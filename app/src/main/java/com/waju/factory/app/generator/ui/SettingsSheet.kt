package com.waju.factory.app.generator.ui

import android.content.Context.MODE_PRIVATE
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.waju.factory.app.generator.domain.model.MiniApp
import com.waju.factory.app.generator.viewModel.MainViewModel

@Composable
fun SettingsSheet(onDismiss: () -> Unit) {

    val viewModel: MainViewModel = viewModel()

    BackHandler(onBack = onDismiss)

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(20.dp)
        )

        LazyColumn {
            items(viewModel.getLocalData()) { app ->
                AppCard(app)
            }
        }
    }
}

@Composable
fun AppCard(app: MiniApp) {

    // var showApp by remember { mutableStateOf(false) }
    val viewModel: MainViewModel = viewModel()

    Column (
        modifier = Modifier
            .clickable(onClick = {  })
            .padding(16.dp)
    ) {
        Text(app.title, fontWeight = FontWeight.Bold)
        Text(app.id)
        Text(viewModel.getAppDetailData(app.id) ?: "No data")
    }
}
