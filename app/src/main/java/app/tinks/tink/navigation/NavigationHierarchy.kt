package app.tinks.tink.navigation

fun MyNavKey.topDestination(): MyNavKey = when (this) {
    ScreenWeightHistory -> ScreenWeight
    ScreenLearntZi,
    ScreenStoryList,
    is ScreenStoryDetail -> ScreenZi
    ScreenSettings -> ScreenSettings
    else -> this
}
