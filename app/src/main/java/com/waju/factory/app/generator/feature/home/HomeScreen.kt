package com.waju.factory.app.generator.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.waju.factory.app.generator.data.model.MiniApp
import com.waju.factory.app.generator.data.session.MiniAppSession

@Composable
fun HomeScreen(
    apps: List<MiniApp>,
    session: MiniAppSession,
    viewModel: MainViewModel = viewModel(),
    goSettings: () -> Unit,
    goViewer: (MiniApp) -> Unit
) {
    val showNewAppDialog by viewModel.showNewAppDialog.collectAsState()
    val newAppTitle by viewModel.newAppTitle.collectAsState()
    val htmlContent by viewModel.htmlContent.collectAsState()

    HomeContent(
        apps = apps,
        onAppClick = goViewer,
        onAppDelete = { app -> viewModel.deleteApp(app) },
        onAddNew = { viewModel.showDialog() },
        onAppDetailDataDelete = { app ->
            viewModel.deleteAppDetailData(app.id)
        },
        goSettings = goSettings,
        showNewAppDialog = showNewAppDialog,
        newAppTitle = newAppTitle,
        htmlContent = htmlContent,
        onDismissRequest = { viewModel.dismissDialog() },
        onTitleChange = { viewModel.updateNewAppTitle(it) },
        onHtmlContentChange = { viewModel.updateHtmlContent(it) }
    )
}