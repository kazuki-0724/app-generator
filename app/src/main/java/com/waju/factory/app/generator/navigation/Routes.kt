package com.waju.factory.app.generator.navigation

import com.waju.factory.app.generator.data.model.MiniApp
import kotlinx.serialization.Serializable

@Serializable
sealed class ScreenRoute {
    @Serializable
    data object HomeRoute: ScreenRoute() // 引数が不要な場合はdata object
    @Serializable
    data object SettingsRoute: ScreenRoute()
    @Serializable
    data class ViewerRoute(val appId: String): ScreenRoute()
}
