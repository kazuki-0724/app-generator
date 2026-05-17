package com.waju.factory.app.generator.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.waju.factory.app.generator.ui.GridViewScreen
import kotlinx.serialization.Serializable

@Serializable
sealed class ScreenRoute {
    data object Home: ScreenRoute() // 引数が不要な場合はdata object
    data class Detail( // 引数が必要な場合はdata class
        val id: String,
        val title: String
    ): ScreenRoute()
}
