package app.tinks.tink.yt

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import app.tinks.tink.ui.theme.TinkTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Authored for the new screen; execution requires an explicit UI-test request.
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class YtLibraryScreenTest {
    @get:Rule val compose = createComposeRule()
    private val item = YtDownload(1, "LbCHAQ7HKu4", YtDownloadKind.AUDIO, "a", title = "An episode", status = YtDownloadStatus.READY)

    @Test fun bulkDeletionRequiresConfirmationAndDeduplicatesSharedEpisode() {
        var deleted = emptyList<YtDownload>()
        val subscriptions = listOf(YtSubscription("a", "First"), YtSubscription("b", "Second"))
        val entries = subscriptions.map { YtPlaylistEntry(it.playlistId, item.videoId, item.title, 0) }
        compose.setContent { TinkTheme { YtLibraryContent(YtLibraryState(listOf(item.copy(standalone = false)), subscriptions, entries),
            onDeleteSelected = { deleted = it }) } }
        compose.onNodeWithTag("yt_select_items").performClick()
        compose.onAllNodesWithTag("yt_select_1")[0].performClick()
        compose.onNodeWithText("1 selected").assertIsDisplayed()
        compose.onNodeWithTag("yt_delete_selected").performClick()
        assertTrue(deleted.isEmpty())
        compose.onNodeWithTag("yt_confirm_delete_selected").performClick()
        assertEquals(listOf(1), deleted.map { it.id })
    }

    @Test fun playlistRemovalOffersExplicitDownloadDeletion() {
        var removal: Pair<String, Boolean>? = null
        compose.setContent { TinkTheme { YtLibraryContent(YtLibraryState(subscriptions = listOf(YtSubscription("p", "Playlist"))),
            onUnsubscribe = { id, delete -> removal = id to delete }) } }
        compose.onNodeWithTag("yt_tab_1").performClick()
        compose.onNodeWithContentDescription("Remove subscription").performClick()
        compose.onNodeWithTag("yt_remove_downloads").assertIsOn()
        assertNull(removal)
        compose.onNodeWithTag("yt_confirm_remove_playlist").performClick()
        assertEquals("p" to true, removal)
    }

    @Test fun episodeHasNoPlayButtonAndPlaylistTitleUsesHeaderWidth() {
        compose.setContent { TinkTheme { YtLibraryContent(YtLibraryState(
            downloads = listOf(item.copy(standalone = false)),
            subscriptions = listOf(YtSubscription("p", "A long playlist title that needs the full library width")),
            entries = listOf(YtPlaylistEntry("p", item.videoId, item.title, 0))),
            playback = YtPlaybackState(connected = true)) } }
        compose.onNodeWithTag("yt_play_1").assertDoesNotExist()
        compose.onNodeWithTag("yt_episode_1").assertHasClickAction()
        val header = compose.onNodeWithTag("yt_group_header_p", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val title = compose.onNodeWithTag("yt_group_title_p", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(header.width, title.width, 1f)
    }

    @Test fun tappingEpisodeInSelectionModeSelectsWithoutPlaying() {
        var played = false
        compose.setContent { TinkTheme { YtLibraryContent(YtLibraryState(downloads = listOf(item)),
            playback = YtPlaybackState(connected = true), onPlay = { _, _ -> played = true }) } }
        compose.onNodeWithTag("yt_select_items").performClick()
        compose.onNodeWithTag("yt_episode_1").performClick()
        compose.onNodeWithText("1 selected").assertIsDisplayed()
        assertFalse(played)
    }

    @Test fun emptyLibraryOffersAddLink() {
        compose.setContent { TinkTheme { YtLibraryContent(YtLibraryState()) } }
        compose.onNodeWithText("Ready when you are").assertIsDisplayed()
        compose.onNodeWithTag("yt_add_link").performClick()
        compose.onNodeWithTag("yt_add_confirm").assertIsNotEnabled()
    }

    @Test fun playbackAndDeletionCallbacksUseSelectedEpisode() {
        var played = 0
        var deleted = 0
        compose.setContent { TinkTheme { YtLibraryContent(YtLibraryState(downloads = listOf(item)),
            playback = YtPlaybackState(connected = true), onPlay = { episode, _ -> played = episode.id }, onDelete = { deleted = it.id }) } }
        compose.onNodeWithTag("yt_episode_1").performClick()
        assertEquals(1, played)
        compose.onNodeWithTag("yt_episode_menu_1").performClick()
        compose.onNodeWithText("Delete download").performClick()
        assertEquals(0, deleted)
        compose.onNodeWithTag("yt_confirm_delete").performClick()
        assertEquals(1, deleted)
    }

    @Test fun playerExposesPauseNextAndSpeed() {
        var next = false
        var speed = 1f
        compose.setContent { TinkTheme { YtLibraryContent(YtLibraryState(downloads = listOf(item)),
            YtPlaybackState(true, 1, "An episode", true, duration = 100000, hasNext = true),
            onNext = { next = true }, onSpeed = { speed = it }) } }
        compose.onNodeWithTag("yt_player_next").performClick()
        assertTrue(next)
        compose.onNodeWithTag("yt_player_speed").performClick()
        compose.onNodeWithText("1.5×").performClick()
        assertEquals(1.5f, speed)
    }
    @Test fun playlistFormExposesCronAndLatestCountAndRejectsInvalidCron() {
        compose.setContent { TinkTheme { YtLibraryContent(YtLibraryState()) } }
        compose.onNodeWithTag("yt_add_link").performClick()
        compose.onNodeWithTag("yt_add_playlist").performClick()
        compose.onNodeWithTag("yt_add_url").performTextInput("https://www.youtube.com/playlist?list=PL64Si-sSRnvZxhAwf2kxsqQXsg8Qd9FDG")
        compose.onNodeWithTag("yt_cron").performScrollTo().performTextReplacement("invalid")
        compose.onNodeWithTag("yt_latest_count").performScrollTo().assertTextContains("1")
        compose.onNodeWithTag("yt_add_confirm").performClick()
        compose.onNodeWithTag("yt_add_error").performScrollTo().assertTextContains("Use five cron fields", substring = true)
    }

    @Test fun subscriptionSettingsDefaultToAutoDeleteAndOfferSpeed() {
        compose.setContent { TinkTheme { YtLibraryContent(YtLibraryState()) } }
        compose.onNodeWithTag("yt_add_link").performClick()
        compose.onNodeWithTag("yt_add_playlist").performClick()
        compose.onNodeWithTag("yt_auto_delete").performScrollTo().assertIsOn().performClick().assertIsOff()
        compose.onNodeWithTag("yt_subscription_speed").performScrollTo().performClick()
        compose.onNodeWithText("1.5×").performClick()
        compose.onNodeWithTag("yt_subscription_speed").assertTextContains("Playback speed: 1.5×")
    }

    @Test fun playAllUsesSelectedSubscription() {
        var playedGroup = ""
        compose.setContent { TinkTheme { YtLibraryContent(YtLibraryState(downloads = listOf(item),
            subscriptions = listOf(YtSubscription("p", "Playlist")), entries = listOf(YtPlaylistEntry("p", item.videoId, item.title, 0))),
            playback = YtPlaybackState(connected = true), onPlayGroup = { playedGroup = it }) } }
        compose.onNodeWithTag("yt_play_group_p").performClick()
        assertEquals("p", playedGroup)
    }

    @Test fun editsLabelsForAnIndividualEpisode() {
        var saved = ""
        compose.setContent { TinkTheme { YtLibraryContent(YtLibraryState(downloads = listOf(item)),
            onLabels = { episode, labels -> assertEquals(item.id, episode.id); saved = labels }) } }
        compose.onNodeWithTag("yt_episode_menu_1").performClick()
        compose.onNodeWithTag("yt_edit_labels_1").performClick()
        compose.onNodeWithTag("yt_labels_input").performTextInput("History, Interviews")
        compose.onNodeWithTag("yt_labels_save").performClick()
        assertEquals("History, Interviews", saved)
    }

    @Test fun labelFilterHidesOtherIndividualEpisodes() {
        compose.setContent { TinkTheme { YtLibraryContent(YtLibraryState(downloads = listOf(
            item.copy(labels = "History"), item.copy(id = 2, title = "Science episode", labels = "Science")))) } }
        compose.onNodeWithTag("yt_label_history").performClick()
        compose.onNodeWithTag("yt_episode_1").assertExists()
        compose.onNodeWithTag("yt_episode_2").assertDoesNotExist()
    }

    @Test fun manualCheckRemainsAvailableWhenAutomaticChecksArePaused() {
        var checked = ""
        compose.setContent { TinkTheme { YtLibraryContent(YtLibraryState(subscriptions = listOf(YtSubscription("p", "Paused playlist", enabled = false))), onCheck = { checked = it }) } }
        compose.onNodeWithTag("yt_tab_1").performClick()
        compose.onNodeWithTag("yt_check_now_p").performScrollTo().assertIsEnabled().performClick()
        assertEquals("p", checked)
    }

    @Test fun showsCheckProgressAndDisablesDuplicateTap() {
        compose.setContent { TinkTheme { YtLibraryContent(YtLibraryState(subscriptions = listOf(YtSubscription("p", "Playlist")),
            checks = mapOf("p" to YtCheckState("Loading playlist…", true)))) } }
        compose.onNodeWithTag("yt_tab_1").performClick()
        compose.onNodeWithTag("yt_check_status_p").performScrollTo().assertTextEquals("Loading playlist…")
        compose.onNodeWithTag("yt_check_now_p").performScrollTo().assertIsNotEnabled()
    }

    @Test fun playlistFormOffersOnlyAudioAndVideo() {
        compose.setContent { TinkTheme { YtLibraryContent(YtLibraryState()) } }
        compose.onNodeWithTag("yt_add_link").performClick()
        compose.onNodeWithTag("yt_add_playlist").performClick()
        compose.onNodeWithTag("yt_subscription_format_video").performClick().assertIsSelected()
        compose.onNodeWithTag("yt_subscription_format_both").assertDoesNotExist()
        compose.onNodeWithTag("yt_subscription_format_audio").performClick().assertIsSelected()
    }
    @Test fun oneTimeImportHidesRecurringScheduleAndCount() {
        compose.setContent { TinkTheme { YtLibraryContent(YtLibraryState()) } }
        compose.onNodeWithTag("yt_add_link").performClick()
        compose.onNodeWithTag("yt_add_playlist").performClick()
        compose.onNodeWithTag("yt_download_all_once").performScrollTo().performClick().assertIsOn()
        compose.onNodeWithTag("yt_cron").assertDoesNotExist()
        compose.onNodeWithTag("yt_latest_count").assertDoesNotExist()
        compose.onNodeWithTag("yt_download_all_once").performClick()
        compose.onNodeWithTag("yt_cron").assertExists()
    }
    @Test fun sharedVideoOpensPrefilledDialogWithoutStartingDownload() {
        var downloads = 0
        var consumed = false
        compose.setContent { TinkTheme { YtLibraryContent(YtLibraryState(),
            sharedUrl = "https://www.youtube.com/watch?v=LbCHAQ7HKu4", shareRequestId = "share-1",
            onShareConsumed = { consumed = true }, onAdd = { _, _, _, _, _, _, _, _, _, _ -> downloads++ }) } }
        compose.onNodeWithText("Add to your library").assertIsDisplayed()
        compose.onNodeWithTag("yt_add_url").assertTextContains("https://www.youtube.com/watch?v=LbCHAQ7HKu4")
        assertEquals(0, downloads)
        compose.onNodeWithText("Cancel").performClick()
        assertTrue(consumed)
        compose.onNodeWithTag("yt_add_url").assertDoesNotExist()
    }

    @Test fun anotherShareReplacesAnOpenDialogInput() {
        val request = androidx.compose.runtime.mutableStateOf("first")
        compose.setContent { TinkTheme { YtLibraryContent(YtLibraryState(), sharedUrl = if (request.value == "first")
            "https://www.youtube.com/watch?v=LbCHAQ7HKu4" else "https://www.youtube.com/watch?v=sO35ixeX0k8", shareRequestId = request.value) } }
        compose.onNodeWithTag("yt_add_url").assertTextContains("LbCHAQ7HKu4", substring = true)
        compose.runOnIdle { request.value = "second" }
        compose.onNodeWithTag("yt_add_url").assertTextContains("sO35ixeX0k8", substring = true)
    }
    @Test fun playlistCanCollapseAndExpandWithoutHidingItsHeaderActions() {
        val episode = item.copy(standalone = false, downloadedAt = 1000)
        compose.setContent { TinkTheme { YtLibraryContent(YtLibraryState(downloads = listOf(episode),
            subscriptions = listOf(YtSubscription("p", "Playlist")), entries = listOf(YtPlaylistEntry("p", item.videoId, item.title, 0))),
            playback = YtPlaybackState(connected = true)) } }
        compose.onNodeWithTag("yt_episode_1").assertExists()
        compose.onNodeWithTag("yt_download_date_1", useUnmergedTree = true).assertTextContains("Downloaded", substring = true)
        compose.onNodeWithTag("yt_toggle_group_p").performClick()
        compose.onNodeWithTag("yt_episode_1").assertDoesNotExist()
        compose.onNodeWithTag("yt_play_group_p").assertIsEnabled()
        compose.onNodeWithTag("yt_toggle_group_p").performClick()
        compose.onNodeWithTag("yt_episode_1").assertExists()
    }
}
