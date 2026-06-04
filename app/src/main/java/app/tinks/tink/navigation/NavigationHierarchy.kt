package app.tinks.tink.navigation

fun MyNavKey.topDestination(): MyNavKey = when (this) {
    ScreenWeightHistory -> ScreenWeight
    ScreenLotteryHistoryStats -> ScreenLottery
    ScreenLearntZi,
    ScreenStoryList,
    is ScreenStoryDetail -> ScreenZi
    ScreenSettings -> ScreenSettings
    else -> this
}
