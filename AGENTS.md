# Repository Instructions

`Tink-Super-App` is the Android client for Tink, built with Kotlin and Jetpack Compose.

## Rules

- Keep Retrofit interfaces aligned with `api-contract/dist/openapi.yaml`.
- Follow existing Compose, Hilt, repository, and view model patterns.
- Keep API models close to the feature package unless an existing shared model is already used.
- Do not commit local Android Studio files, build outputs, keystores, or secrets.

## Project Skills

Official Google Android skills are available under `.agent/skills/android/`. When a task matches edge-to-edge UI, testing setup, Navigation 3, Compose adaptive UI, or Compose theming/styles work, review the relevant local `SKILL.md` before implementing.

## Verification

Run the local unit, Robolectric, and Compose behavior tests when Java is available:

```sh
./gradlew testDebugUnitTest
```

Run local unit test coverage:

```sh
./gradlew jacocoDebugUnitTestReport
```

Run instrumented tests when an emulator or device is available:

```sh
./gradlew connectedDebugAndroidTest
```

Run Compose Preview Screenshot Testing:

```sh
./gradlew updateDebugScreenshotTest
./gradlew validateDebugScreenshotTest
```

Use `updateDebugScreenshotTest` to create or intentionally update screenshot baselines before `validateDebugScreenshotTest`.

The Android GitHub Actions workflow publishes unit/UI, coverage, and screenshot HTML reports under GitHub Pages run paths. The screenshot report is copied from `app/build/reports/screenshotTest/preview/debug` so rendered screenshot results can be opened directly in the browser.

GitHub Actions installs Java and is the authoritative Android gate when local Java is unavailable.
