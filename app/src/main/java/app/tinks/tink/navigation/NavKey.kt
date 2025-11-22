package app.tinks.tink.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey

sealed interface MyNavKey: NavKey {
    val label: String
    val icon: ImageVector
}

// 使用 data object 定义静态目的地
data object ScreenA : MyNavKey {
    override val label = "主页 A"
    override val icon = Icons.Filled.Home
}

data object ScreenB : MyNavKey {
    override val label = "设置 B"
    override val icon = Icons.Filled.Settings
}

data object ScreenC : MyNavKey {
    override val label = "体重 C"
    override val icon = Icons.Filled.Build
}

val allDestinations = listOf(ScreenA, ScreenB, ScreenC)
