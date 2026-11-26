package app.tinks.tink.navigation

fun MyNavKey.topDestination(): MyNavKey = when (this) {
    ScreenRandomPlay, ScreenRandomWheelEditor -> ScreenRandom
    ScreenWeightHistory -> ScreenWeight
    ScreenLotteryHistoryStats -> ScreenLottery
    ScreenLearntZi,
    ScreenWrittenZi,
    is ScreenZiHandwriting,
    ScreenStoryList,
    is ScreenStoryDetail -> ScreenZi
    ScreenSettings -> ScreenSettings
    else -> this
}
