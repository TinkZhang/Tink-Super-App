# YT: local YouTube downloads

Scope: single public YouTube videos, audio-only M4A or video-with-audio MP4 up to 720p.
Subscriptions, cron, account access and a built-in player are subsequent slices. Open delegates
to an installed media player using a temporary, read-only FileProvider grant.

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
