# Repository Instructions

`Tink-Super-App` is the Android client for Tink, built with Kotlin and Jetpack Compose.

## Rules

- Work directly on `main` and push verified commits to `origin/main`.

- Keep Retrofit interfaces aligned with `api-contract/dist/openapi.yaml`.
- Follow existing Compose, Hilt, repository, and view model patterns.
- Keep API models close to the feature package unless an existing shared model is already used.
- Every new or changed Android feature must include automated test updates with the implementation. Do not treat feature work as complete until the relevant unit, Robolectric Compose UI, and screenshot coverage has been added or deliberately documented as not applicable.
- Do not commit local Android Studio files, build outputs, keystores, or secrets.

## Project Skills

Official Google Android skills are available under `.agent/skills/android/`. When a task matches edge-to-edge UI, testing setup, Navigation 3, Compose adaptive UI, or Compose theming/styles work, review the relevant local `SKILL.md` before implementing.

## Feature Test Policy

### UI test execution is opt-in

- Run UI tests only when the user explicitly asks for them. Otherwise, DO NOT run UI tests.
- This applies locally and in CI to Robolectric Compose/UI tests, screenshot tests and baseline generation, instrumented UI tests, and Appium.
- Implementing a feature, building, installing, launching, fixing or pushing the app does not authorize UI tests.
- You may author UI tests without running them. Non-UI tests and build/install/launch operations remain allowed.
- Use explicit non-UI test selection. Do not run unfiltered `testDebugUnitTest`, coverage tasks that depend on it, or UI CI workflows unless UI tests were requested.
- This takes precedence over all UI-test execution guidance below and in referenced skills/playbooks.

When implementing a feature, update tests at the same time as the production code:

- Unit tests: cover DTO/domain mapping, repository request/response behavior, UI state transformation, and view model state transitions.
- Robolectric Compose UI tests: cover user-visible screen behavior, callbacks, empty/loading/error states, and navigation events that do not require a real device.
- Screenshot tests: add or update Compose Preview Screenshot Testing baselines for changed screens, important states, and reusable feature UI components.
- Navigation tests: when adding or changing destinations, cover top-level destination mapping and back-stack behavior with pure Kotlin tests where possible.
- CI/reporting: non-UI checks may run by default. Mixed suites, UI tests, screenshot checks, and Appium dispatch must be gated on an explicit user request. Publish reports for checks that were authorized and run.
- Black-box Appium handoff: Android CI owns building and uploading the debug APK artifact (`tink-android-debug-apk`). `tink-test` should download this artifact and must not checkout or build the Android project for Appium runs.

If a test category is not applicable, write the reason in the Linear update. Avoid skipping a category silently.

For screenshot tests, use the GitHub Actions runner as the source of truth for baselines when local rendering differs from CI rendering. Screenshot reports should remain viewable from the published GitHub Pages report.

## Verification

Only when the user explicitly requests UI tests, run the mixed unit/Robolectric/Compose suite:

```sh
./gradlew testDebugUnitTest
```

Only within an explicit UI-test request, run coverage (which depends on the mixed suite):

```sh
./gradlew jacocoDebugUnitTestReport
```

Only when explicitly requested, run instrumented UI tests:

```sh
./gradlew connectedDebugAndroidTest
```

Only when explicitly requested, run Compose Preview Screenshot Testing:

```sh
./gradlew updateDebugScreenshotTest
./gradlew validateDebugScreenshotTest
```

Use `updateDebugScreenshotTest` to create or intentionally update screenshot baselines before `validateDebugScreenshotTest`.

Build the local debug APK used by black-box Appium tests:

```sh
scripts/build-appium-debug-apk.sh
```

Only when the user explicitly requests UI tests, run the Appium suites from `../tink-test`:

```sh
scripts/run_android_appium_local.sh --suite e2e
scripts/run_android_appium_local.sh --suite mock
```

The Android GitHub Actions workflow publishes unit/UI, coverage, and screenshot HTML reports under GitHub Pages run paths. The screenshot report is copied from `app/build/reports/screenshotTest/preview/debug` so rendered screenshot results can be opened directly in the browser.

GitHub Actions installs Java and is the authoritative Android gate when local Java is unavailable.
