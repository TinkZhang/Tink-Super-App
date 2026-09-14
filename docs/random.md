# Random

Native, local-only Dice, weighted Wheel and Coin. The user approved implementation from the reviewed interactive design on 2026-09-06 and explicitly requested fewer instructional labels on settings; no further design/Figma step was requested.

## Behavior

- Drawer → 随机 opens settings. Settings retain the selected type, 1–6 dice, a wheel of 2–12 labeled sections with integer weights 1–100, and independent sound / Chinese speech preferences in local app storage.
- Start opens Play without rolling automatically. Only the objects are visible. Tap the central region to play again; taps during animation are ignored.
- System Back (including Android edge gestures and accessibility Back) returns to settings. The drawer cannot intercept gestures in Play.
- Play hides system bars and keeps the screen on only while resumed. Pausing or leaving cancels a pending roll and sound, releases the screen flag, and restores previous system-bar behavior. Returning from the background never automatically rolls or repeats speech.
- Chinese result semantics remain available to TalkBack. Spoken results use an installed offline Chinese TTS voice. If none is available, settings shows “本机中文语音不可用”; visual results and sound effects continue to work.
- Wheel sector sizes and selection probabilities use the same weights. The stationary pointer lands at the selected sector's center. The draw is selected once before animation.
- Effects are original, procedurally generated 22.05 kHz mono PCM assets; no network or runtime audio permissions are needed. System speech-service visibility is declared in the manifest.
- Editor drafts survive configuration recreation. Empty labels and invalid weights block saving; removing options and discarding edits require confirmation.

## Testing strategy

The existing stack is Hilt, JUnit4, Robolectric/Compose, Compose Preview Screenshot Testing, Jacoco and external Appium (`tink-test`). It is retained without additional testing dependencies.

- Pure tests exercise weighted ticket boundaries, wheel pointer math, dice bounds and Chinese formatting.
- ViewModel tests cover repeated taps, cancellation, settings retention and reduced motion.
- Robolectric tests cover local persistence / corrupt data, clean settings, Play semantics and input state, editor draft restoration / validation / confirmation, and screen-flag restoration.
- Navigation hierarchy tests cover the new destinations and drawer membership.
- Screenshot previews cover settings, editor, three Play types, small phones, compact/medium/expanded sizes, dark mode and large text. System bar behavior is checked on the emulator, not by LayoutLib.
- `tink-test` adds local-only BDD scenarios to both mock and E2E harness configurations. The Random feature itself makes no API requests; API-contract, backend and API E2E changes are not applicable.

## Verification commands

Use JDK 21 (the Android Studio bundled JBR) and the local Android SDK.

```sh
./gradlew testDebugUnitTest
./gradlew jacocoDebugUnitTestReport
./gradlew updateDebugScreenshotTest
./gradlew validateDebugScreenshotTest
scripts/build-appium-debug-apk.sh
```

Run `scripts/run_android_appium_local.sh --suite mock -- -k random` and `--suite e2e -- -k random` from the `tink-test` worktree, setting `TINK_ANDROID_REPO` to this Android worktree. The runner uses an isolated emulator app installation.

Linear synchronization is pending because the connector requires reauthentication. No Linear identifier has been invented.

## Local verification (2026-09-06)

- Random unit/Robolectric tests: 14 passed. Navigation hierarchy: 5 passed.
- Debug APK build and Jacoco report generation: passed (coverage report uses the focused 19-test run).
- Random screenshot baselines: 43 passed, including small screens, landscape, dark mode and large text.
- Full Android unit suite: 176 passed, 2 failed. `LeetKeeperScreenTest.dashboard_rendersOngoingPlanAndProblems` and `SecureWebScreenTest.screen_requestsAuthenticationOnLaunch` both fail identically on untouched commit `595814c`.
- Full screenshot suite: 58 passed, 1 failed. `DiaryLoomHomeScreenshotPreview` also fails on untouched commit `595814c`; its existing reference image has been preserved.
- Chinese speech requires an installed offline Chinese system voice. Automated checks validate Chinese result text and lifecycle behavior; audible pronunciation has not been verified on a Chinese-voice-equipped physical device.

The Android changes are isolated on `codex/random-feature`; existing uncommitted work in the original checkout was preserved. Companion black-box tests live on the same branch name in `tink-test`.
