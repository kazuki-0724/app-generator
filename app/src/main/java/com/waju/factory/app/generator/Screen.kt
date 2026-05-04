package com.waju.factory.app.generator

sealed class Screen(val route: String) {
    object Grid : Screen("grid_screen")
    object Detail : Screen("detail_screen/{itemId}") {
        // 引数付きのルートを生成するヘルパー関数
        fun createRoute(itemId: Int) = "detail_screen/$itemId"
    }
}