# YT: local YouTube downloads

Scope: public YouTube audio/video downloads, an offline audio library/player, and playlist
subscriptions with title regex filters. Video remains MP4 up to 720p and plays in Tink’s built-in video player. No backend or YouTube API key is required.

## Audio library and subscriptions

- Library, Playlists and Downloads tabs replace the initial download-first screen. The user
  requested a podcast-style layout using the previously approved existing Tink visual style.
- Add a single video link for ungrouped audio or video. Add a playlist link (including a watch
  URL with `list=`) for grouped audio or video. A trailing comma on the shared example is tolerated.
- Playlist filters use substring regex matching, case-sensitive by default; `(?i)` enables
  case-insensitive matching. Blank means all titles. Invalid patterns are rejected before saving.
- An initial check and the **Check now** button use network-constrained WorkManager jobs.
  Automatic checks use a five-field numeric Unix cron expression in the device time zone.
  Examples: `0 8 * * *` (08:00 daily), `0 */6 * * *` (every six hours), `30 7 * * 1-5`
  (07:30 weekdays). Supports `*`, numbers, ranges, lists and steps; Sunday is 0 or 7.
  Due schedules are evaluated by network-constrained WorkManager polling every 15 minutes;
  Android battery/network restrictions can delay them further. Missed occurrences coalesce
  into one check, not a catch-up storm. A persisted compare-and-set prevents double claims.
  Time-zone changes reset the next due time in the new device zone. Existing daily workers
  are upgraded on app startup without duplicating their unique work registrations.
- Each subscription has a **Latest episodes** count (1–500, default 1 for new subscriptions).
  take the first N playlist entries in their supplied order,
  then skip downloaded/queued/dismissed IDs. Repeated checks never backfill older items just
  because the newest N are already present. Existing files and previously queued downloads
  are kept when the count/filter changes.
- Playlist order is authoritative: the first entry is newest. Checks request only the first N
  entries and apply the title regex within that set. No per-video upload-date requests are made.
  Existing date-cache rows remain for migration compatibility and do not affect selection/playback.
  Playlist URL fields display `https://www.youtube.com/playlist?list=…`.
- Selected new entries enter a durable local queue. At most four transfer jobs are scheduled at
  once and native downloads execute serially. Subscription transfers use ordinary background
  jobs; direct downloads retain user-initiated jobs. Background work is subject to Android quotas.
- One file can belong to multiple playlists. Adding it as an individual link also exposes it
  ungrouped. Repeated checks do not duplicate files. Playlist ordering is refreshed on checks.
- Delete removes the file from all groups and remembers its dismissal so it is not automatically
  re-downloaded. Adding the individual link explicitly can download it again. Removing a playlist
  stops checks and offers **Delete downloads too** (selected by default). Confirming deletes matching audio/video files and database rows from all groups; unchecking keeps active downloads as individual episodes. Dismissed memberships are never promoted. Pausing checks does not
  cancel downloads already queued. Edited filters apply prospectively; existing files are kept.
- Media3 ExoPlayer lives in a MediaSessionService: play/pause, next, seeking, back 15 seconds,
  speeds from 0.5× to 3×, audio focus, headphone-disconnect handling, and system media controls.
- Playback position is saved periodically and when paused/transitioned; select an episode to
  resume after process restart. Speed is remembered per playlist, with a separate individual
  queue preference. Playback continues when leaving YT or locking the device.
- Subscription Add/Edit dialogs include **Auto-delete after playback**, ON by default, and
  a default playback speed (0.5×–3×). Speed changes in the player update the current subscription.
  **Play all · oldest first** starts the downloaded subscription queue from the beginning,
  ordered by reverse playlist position (oldest first), and automatically advances to the next episode.
- Auto-delete runs on natural completion, including the final episode, not pause, manual skip,
  playback failure or the **Mark played** action. It removes local media/artwork and saves dismissal
  tombstones so checks do not download the episode again. A file also saved individually, or shared
  with a subscription whose auto-delete is OFF, is retained; only its finished group membership is
  dismissed. Subscription-only episodes in the combined library retain subscription settings;
  for shared episodes, opening a particular subscription selects that group's speed/settings.
- Episode cards, playlist headers and the player display YouTube artwork with an icon fallback.
  New downloads save an optional local cover for offline use. Existing downloads use their YouTube
  thumbnail online/cache; playlist artwork comes from playlist metadata or an episode fallback.
- Mark played/unplayed and delete actions manage local content. Audio and video both play locally inside Tink.
- Room migrations 1→2→3→4→5→6→7→8 preserve downloads, subscriptions and dismissals. Version 3 adds
  cron/count/next-due fields and the upload-date cache. Existing subscriptions default to
  `0 8 * * *` and latest 10. Version 4 adds auto-delete (ON), subscription speed (1×),
  and artwork. Existing files are retained during migration; deletion requires future playback completion.

References: [Media3 background playback](https://developer.android.com/media/media3/session/background-playback)
and [yt-dlp playlist options](https://github.com/yt-dlp/yt-dlp#usage-and-options).

## Verification for the audio-library slice

Non-UI tests cover migration of a version-1 database with an existing READY download, retained
playback state/dismissals, playlist URL validation, title matching, queue membership/order,
bounded background scheduling, cron parsing/time zones/DST/leap days, migration of existing
subscriptions, newest-N selection, repeated/overlapping-playlist deduplication, oldest-first queues,
auto-delete completion/retention rules, artwork URL validation, and saved listening settings. Existing downloader,
view-model and update-policy tests remain selected explicitly.
Compose behavior tests, screenshot previews and Appium scenarios were authored/updated but
not executed, following the user's UI-test policy. Device playback, screen-lock controls and
live playlist fetching still need manual verification. No API tests apply. Linear remains
unavailable due to connector authentication; no issue comments or CI workflows were triggered.

The user approved using the existing Tink screen style in this task instead of a Figma design.
Linear was inaccessible because its connector required reauthentication. No API or backend changes.

## Implementation

- Compose / Hilt ViewModel / repository, matching the existing Android stack.
- Separate Room database `yt-downloads.db`; media under `files/yt/<download-id>/`.
- Both database and files are excluded from cloud backup and device transfer.
- Only canonical single-video YouTube URLs reach yt-dlp. Tracking and playlist arguments are stripped.
- yt-dlp is also restricted to the YouTube extractor. No arbitrary commands or options are exposed.
- Android user-initiated transfer jobs provide background execution, network constraints,
  progress notifications and cancellation. Native downloads are serialized.
- After runtime initialization and before a download, check GitHub for the latest stable yt-dlp.
  Successful checks (updated or already current) are cached on-device for 24 hours. The runner's
  native-work lock keeps updates separate from active downloads. Failed checks log `update.failed`
  and continue with the installed version; the next download retries the check. `update.done`
  includes the installed version. No backend is involved.
- Room persists state before scheduling. Attempt IDs prevent late callbacks from overwriting a retry.
- Network/system interruption can be rescheduled by Android. After reboot, force-stop or lost jobs,
  reopening YT reconciles state and offers Retry. System Task Manager stops are never silently restarted.
- Retry preserves partial files where supported by yt-dlp. READY requires a completed output file
  inside the item's own folder; missing files become retryable.
- Audio and video of the same YouTube ID are separate entries. Repeated submissions are deduplicated.

## Dependencies and source availability

`io.github.junkfood02.youtubedl-android:library:0.18.1` and `ffmpeg:0.18.1` supply
yt-dlp, Python, QuickJS and FFmpeg. The wrapper is GPL-3.0, as declared in its published POM
and [source license](https://github.com/yausername/youtubedl-android/blob/0.18.1/LICENSE).
Using this wrapper does not avoid GPL obligations. Distribution must include applicable notices
and corresponding source under compatible terms, including the native dependency builds.
Native libraries use legacy extraction because the wrapper runs their executables directly.
Upstream source: https://github.com/yausername/youtubedl-android/tree/0.18.1

## Testing strategy

### Capturing download failures

Rebuild and install the **debug** variant. In Android Studio Logcat, select the connected device
and filter by `tag:YTDownload`, with Debug or Verbose messages visible. Start capturing before
tapping Download or Retry, then copy the logs through the failure.

Alternatively, capture from a terminal (stop with Ctrl+C after the failure):

```sh
adb -s 68d00dcc logcat -v threadtime 'YTDownload:V' '*:S' > yt-download.log
```

Each event includes the download ID and attempt ID. Diagnostics cover scheduling and Android job
stop reasons, network state, Python/FFmpeg startup, native executable availability, yt-dlp verbose
output, progress, output verification, and the original exception/stack trace. The wrapper buffers
stderr, so verbose stderr appears when execution completes or fails. Percentage logs are throttled.
Logs are debug-only; URLs and authentication/proxy diagnostic lines are redacted before logging.

### Test coverage

The existing stack is Hilt, JUnit4, Robolectric Compose, Preview Screenshot Testing,
Jacoco, and external Appium BDD. No new testing framework is introduced.

- Unit tests: YouTube URL allowlist, command construction, repository deduplication, scheduling
  failure, cancellation/retry races, missing-file recovery, runner state and ViewModel input restoration.
- Room behavior is exercised with a real in-memory SQLite database through Robolectric.
- Compose tests: empty form, mode selection, errors, cancel/retry/open callbacks.
- Screenshot previews: nine viewport combinations, the Appium viewport, large font, dark theme,
  downloading, completed and failed states.
- Appium: source rejection and audio/video selection are deterministic local scenarios;
  downloading the user's real video is a separately enabled YouTube network test.
- API tests do not apply because YT has no Tink API dependency.

UI test execution is now opt-in: do not run Compose, screenshot, instrumented UI or Appium tests
unless the user explicitly requests them. A request to build/install/launch the app is not UI-test authorization.
Real download test URL:
https://youtu.be/LbCHAQ7HKu4?si=k3IuU5o_SqidYee5

## Individual episode labels

Individual downloads support up to 10 labels of 40 characters each. Use **Edit labels** in
an episode menu, or **Labels** on its Downloads card (including pending/failed transfers).
Separate names with commas; whitespace is normalized and case variants share one group.
Existing labels are offered as shortcuts. Clearing a label only removes that grouping, not the file.
The individual library has label filters and a **Play label** action, queued earliest-saved first.
An episode can appear under several labels while keeping one local download. Labels persist through
restart/retry and are removed with the download. Playlist-only episodes retain their subscription groups.
Version 5 adds an empty labels column without changing files or saved subscription counts. New
subscriptions now default to **1** latest episode; previous selections are retained.

Label normalization, filtering/queue ordering, persistence and migration have non-UI coverage.
Compose callback/filter tests and a labelled-library screenshot preview are authored only; UI execution
and device verification remain opt-in. No API changes apply to this local feature.

## Built-in downloaded video player

Tap **Play video** on a ready download to open a dedicated Tink screen, with a back arrow,
play/pause, seek bar, ±10-second seeking and 0.5×–3× speed controls. The video fits the available
portrait/landscape area without stretching. Video playback pauses the audio player and pauses
when the video screen leaves the foreground; reopening resumes its saved position. Completed
individual videos replay from the start and remain downloaded. Subscription videos follow their auto-delete setting. The screen stays awake only while playing.
Missing or unsupported files show an error and preserve the download for retry/removal.

The private activity receives a download ID and resolves only an existing READY video under
its own local media directory. It uses the existing Media3 ExoPlayer dependency, releases the
player on destruction and drains ordered position writes. No network/API or external player is needed.
Media3 integration follows [the official player lifecycle guidance](https://developer.android.com/media/media3/exoplayer/hello-world).

Non-UI tests cover video eligibility, saved progress and local-file boundaries. UI controls/error
coverage, screenshot previews and the real-download Appium playback step are authored but not run.
Actual decoder output, rotation and device lifecycle behavior still require manual device verification.

## Subscription formats and visible checks

Subscription Add/Edit includes **Audio** and **Video**. Each subscription downloads one format
per episode. Changing the format preserves prior files. Version 7 converts legacy Audio + Video
settings to Video without deleting existing audio copies.
Existing subscriptions migrate as Audio, retain their count (new subscriptions still default to 1),
and keep all files/settings. Format changes apply to the next check and keep prior downloads.
Room version 6 tracks membership and dismissal by playlist/video ID/format. Deleting or finishing
one format does not suppress or remove the other. Existing dismissal tombstones remain audio-only.
The library shows video episodes in their subscription; **Play video · oldest first** uses reverse playlist
order, automatically advances and applies the group's speed/auto-delete settings.

**Check now** works with paused automatic schedules. It reuses an already queued/running manual request, awaits enqueue success, and exposes WorkManager state:
queued/waiting for network or Android, waiting for the downloader, loading playlist, upload-date
lookup count, and selection. Successful checks report the number of new download files queued or
that there are no new matching downloads. Failed checks show errors and permit retry; manual checks
have no silent retry/backoff. Periodic retries retain their existing bounded retry policy.
Regular checks inspect only the first latestCount playlist entries. No upload-date lookups are performed. `YTDownload` includes
`playlist.requested`, `playlist.start`, `playlist.date`, `playlist.done`, and `playlist.failed`.

Non-UI coverage includes format expansion/deduplication, format-specific dismissal and completion,
legacy migrations, check-status transitions and existing download regression tests. UI tests/previews
and Appium format selection are authored only. No UI tests or live playlist downloads were run.

## Fullscreen and compact controls

The video player's fullscreen icon enters immersive landscape mode. System bars hide, controls
fade after three seconds during playback, and tapping the video shows them. Back exits fullscreen
before leaving the player. Playback stays on the same player through orientation changes;
controls respect display cutouts and gesture insets. Seeking and speed menus keep controls visible.

YT uses icon actions for play, refresh, edit, labels, cancellation and deletion, with accessible
content descriptions and standard touch targets. Navigation retains short labels alongside icons.
Playlist cards show compact format/schedule/count/speed/retention indicators; the info icon expands
URL, regex and check dates. Check progress and failures remain readable text. UI tests and previews
are updated but are not executed without an explicit request.

### Debugging “Waiting for downloader”

Filter Android Studio Logcat by **tag:YTDownload** in a debug build. Capture from before tapping
Check now and keep at least 30 seconds of logs. `gate.wait` and `gate.waiting` include the waiting
operation, current owner, owner download/attempt ID and held duration. `gate.acquired` reports wait
time; `gate.holding` repeats every 15 seconds; `gate.released` and `gate.cancelled` show how it ends.
Playlist logs also identify runtime initialization, update completion, extraction result/time and
sanitized stderr. These diagnostics identify the blocker; no timeout forcibly releases a live native
operation or deletes its partial file. The underlying cause still needs device logs to diagnose.

## Bounded regular checks and one-time imports

**Regular subscriptions:** fetch only the first N playlist entries using `--playlist-end N`,
then apply the regex within that set. The playlist’s supplied order defines newest first.
This intentionally favors playlists with recent episodes near the beginning; newer uploads outside
that head are not considered. The requested count still defaults to 1.

**Download all once:** available in Add/Edit for either audio or video. Fetch the whole flat playlist,
apply the optional title filter and persist every matching item in the local download queue. There
are no per-episode upload-date requests, count limits or recurring checks. The initial import is
network constrained and survives restarts through WorkManager. Existing bounded transfer scheduling
still limits concurrency; 1,000 episodes are queued rather than downloaded concurrently. Playback
uses reverse playlist order, treating the first entry as newest.

A one-time import keeps its group, playback speed and retention settings. Editing its settings does
not automatically re-import; switching from regular to one-time starts an import. The refresh icon
can explicitly retry/check again, with existing and dismissed downloads skipped. Regular and one-time
imports share a per-playlist guard so cron and manual requests cannot overlap their scans; coalesced
requests log `playlist.coalesced`. Room version 8 defaults existing subscriptions to regular mode.

Non-UI tests cover bounded selection, date-free full selection, a 1,000-item persisted import and
retry deduplication, and check coalescing/cancellation. UI tests, screenshot preview and Appium mode
selection are authored only; no UI tests or live playlist imports are run without a user request.

## Share a YouTube link into Tink

Tink registers an exported `ACTION_SEND` / `text/plain` receiver on MainActivity, so YouTube’s
Share sheet can offer Tink. Sharing navigates to YT and opens **Add to your library** with the
allowlisted URL prefilled. Titles surrounding the link are tolerated; video links are canonicalized
and tracking/playlist arguments removed. A pure playlist URL preselects playlist mode. The user
still chooses the format/settings and confirms; receiving a share never downloads automatically.
Unsupported text displays a short message instead of passing another site to yt-dlp.

Cold launch and `onNewIntent` both handle shares. Each has a request ID so sharing the same URL
again is a fresh request. Pending requests are saved across Activity recreation and consumed on
confirmation/dismissal; the launch intent is cleared to prevent accidental replay. Shared text is
bounded and uses the existing YouTube URL allowlists. Android text share targets cannot filter by
URL host, so Tink may also appear for text shared from other apps.

Non-UI tests cover text parsing, allowed intent types, hostile hosts and manifest resolution. Compose
prefill/cancel/re-share tests, a dialog preview and an Appium share-intent scenario are authored only;
no UI tests have been executed. Verify the actual YouTube share sheet manually on the device.

## Playlist order update

Regular checks now use `--playlist-end latestCount` directly. The per-video date extraction loop is
removed entirely, including for cache misses. Regex exclusions, duplicates and dismissals never cause
older entries outside the requested first N to be added. One-time imports still select all matching
entries. Group playback reverses playlist position to preserve oldest-first listening without dates.

## Library grouping, artwork and download dates

Library playlist headers expose expand/collapse controls and item counts. Groups start expanded;
collapsed IDs are saved through screen recreation and remain independent of list scrolling and playback.
The header's play action remains available while collapsed. Thumbnails use `ContentScale.Fit` in their
frames, retaining the entire image's aspect ratio without stretching or cropping; episode covers have
a 96×72 dp frame.

Room version 9 adds nullable `downloadedAt`, written by the successful download completion update
and protected by the same attempt/status guards. Retries clear it and successful re-downloads replace
it. Episode cards display localized download date/time. Existing records have no completion timestamp,
so they show **Added** with their original queue date, rather than inventing a completion time.
Non-UI tests cover completion/retry timestamps and migrations. Collapse/date UI tests and narrow/large-
font screenshot previews are authored only. No UI tests run; device layout remains manually verifiable.


### Library deletion and artwork

- Thumbnail containers have no background tint; images retain their original aspect ratio.
- Library’s checklist icon enters selection mode. Select episode checkboxes across playlists,
  then use the trash icon and confirm. Shared downloads count once and are removed from all groups.
- Removing a playlist first detaches it transactionally, preventing an in-flight check from
  recreating its downloads. File deletion cancels transfers before taking the native lock.
- Failed file deletion retains the database row for retry from Downloads and reports an error.
- Regression coverage: `YtLibraryDeletionTest` exercises actual Room records/files, both formats,
  shared membership tombstones, keep behavior and repeated deletion. Compose selection/removal
  coverage is authored in `YtLibraryScreenTest`; UI execution and screenshot baseline generation
  remain deferred until explicitly requested. Existing Library screenshot previews cover the
  changed thumbnails and header; no new navigation or API tests are needed.
