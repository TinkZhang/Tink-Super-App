package app.tinks.tink.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Looks
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey

sealed interface MyNavKey : NavKey {
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

data object ScreenWeight : MyNavKey {
    override val label = "体重 C"
    override val icon = Icons.Filled.Build
}

data object ScreenWeightHistory : MyNavKey {
    override val label = "体重历史"
    override val icon = Icons.Filled.Build
}

data object ScreenHair : MyNavKey {
    override val label = "理发"
    override val icon = Icons.Filled.Looks
}

data object ScreenLeeter : MyNavKey {
    override val label = "Leeter"
    override val icon = Icons.Filled.Code
}

data object ScreenZi : MyNavKey {
    override val label = "识字"
    override val icon = Icons.Filled.Translate
}

data object ScreenLearntZi : MyNavKey {
    override val label = "已学会的字"
    override val icon = Icons.Filled.Translate
}

data object ScreenMerriam: MyNavKey {
    override val label = "M-W Builder"
    override val icon = Icons.Filled.Bookmark
}

data object ScreenTime: MyNavKey {
    override val label = "Time"
    override val icon = Icons.Filled.Schedule
}

data object ScreenBooks : MyNavKey {
    override val label = "Books"
    override val icon = Icons.Filled.LibraryBooks
}

data object ScreenLottery : MyNavKey {
    override val label = "Lottery"
    override val icon = Icons.Filled.LocalActivity
}

data object ScreenLotteryHistoryStats : MyNavKey {
    override val label = "History & stats"
    override val icon = Icons.Filled.LocalActivity
}

data object ScreenSettings : MyNavKey {
    override val label = "设置"
    override val icon = Icons.Filled.Settings
}

data object ScreenStoryList : MyNavKey {
    override val label = "故事列表"
    override val icon = Icons.Filled.Bookmark
}

data class ScreenStoryDetail(val storyId: String) : MyNavKey {
    override val label = "故事详情"
    override val icon = Icons.Filled.Bookmark
}

val allTopDestinations = listOf(
    ScreenA, ScreenB, ScreenWeight, ScreenHair,
    ScreenLeeter, ScreenZi, ScreenMerriam, ScreenTime, ScreenBooks, ScreenLottery
)
