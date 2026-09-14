package app.tinks.tink.testing

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.tinks.tink.random.*
import app.tinks.tink.ui.theme.TinkTheme
import com.android.tools.screenshot.PreviewTest

@Preview(widthDp = 320, heightDp = 640, name = "small-phone")
@Preview(widthDp = 400, heightDp = 400, name = "compact-short")
@Preview(widthDp = 400, heightDp = 500, name = "compact-medium")
@Preview(widthDp = 400, heightDp = 1000, name = "compact-tall")
@Preview(widthDp = 610, heightDp = 400, name = "medium-short")
@Preview(widthDp = 610, heightDp = 500, name = "medium-medium")
@Preview(widthDp = 610, heightDp = 1000, name = "medium-tall")
@Preview(widthDp = 900, heightDp = 400, name = "expanded-short")
@Preview(widthDp = 900, heightDp = 500, name = "expanded-medium")
@Preview(widthDp = 900, heightDp = 1000, name = "expanded-tall")
annotation class RandomSizes

@PreviewTest @RandomSizes
@Preview(widthDp = 400, heightDp = 500, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "dark")
@Preview(widthDp = 400, heightDp = 500, fontScale = 1.5f, name = "large-text")
@Composable
fun RandomSettingsScreenshots() { TinkTheme(dynamicColor = false) { RandomSettingsScreen(RandomSettings()) } }

@PreviewTest @RandomSizes
@Preview(widthDp = 400, heightDp = 500, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "dark")
@Preview(widthDp = 400, heightDp = 500, fontScale = 1.5f, name = "large-text")
@Composable
fun RandomWheelEditorScreenshots() { TinkTheme(dynamicColor = false) { RandomWheelEditor(RandomSettings(type = RandomType.Wheel)) } }

@PreviewTest @RandomSizes
@Preview(widthDp = 400, heightDp = 500, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "dark")
@Preview(widthDp = 400, heightDp = 500, fontScale = 1.5f, name = "large-text")
@Composable
fun RandomDicePlayScreenshots() { TinkTheme(dynamicColor = false) { RandomPlayScreen(RandomSettings(diceCount = 6), RandomPlayState(result = RandomResult(dice = listOf(1, 2, 3, 4, 5, 6)), completedRound = 1)) } }

@PreviewTest
@Preview(widthDp = 320, heightDp = 640)
@Preview(widthDp = 900, heightDp = 400, name = "landscape")
@Preview(widthDp = 400, heightDp = 500, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "dark")
@Composable
fun RandomWheelPlayScreenshots() { TinkTheme(dynamicColor = false) { RandomPlayScreen(RandomSettings(type = RandomType.Wheel), RandomPlayState(completedRound = 1)) } }

@PreviewTest
@Preview(widthDp = 320, heightDp = 640)
@Preview(widthDp = 400, heightDp = 500, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "dark")
@Composable
fun RandomCoinPlayScreenshots() { TinkTheme(dynamicColor = false) { RandomPlayScreen(RandomSettings(type = RandomType.Coin), RandomPlayState(result = RandomResult(heads = false), completedRound = 1)) } }

@PreviewTest @Preview(widthDp = 320, heightDp = 640)
@Composable
fun RandomWheelSettingsScreenshot() { TinkTheme(dynamicColor = false) { RandomSettingsScreen(RandomSettings(type = RandomType.Wheel)) } }

@PreviewTest @Preview(widthDp = 320, heightDp = 640)
@Composable
fun RandomCoinSettingsScreenshot() { TinkTheme(dynamicColor = false) { RandomSettingsScreen(RandomSettings(type = RandomType.Coin)) } }
