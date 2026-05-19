# Repository Instructions

`Tink-Super-App` is the Android client for Tink, built with Kotlin and Jetpack Compose.

## Rules

- Keep Retrofit interfaces aligned with `api-contract/dist/openapi.yaml`.
- Follow existing Compose, Hilt, repository, and view model patterns.
- Keep API models close to the feature package unless an existing shared model is already used.
- Every new or changed Android feature must include automated test updates in the same branch. Do not treat feature work as complete until the relevant unit, Robolectric Compose UI, and screenshot coverage has been added or deliberately documented as not applicable.
- Do not commit local Android Studio files, build outputs, keystores, or secrets.

## Project Skills

Official Google Android skills are available under `.agent/skills/android/`. When a task matches edge-to-edge UI, testing setup, Navigation 3, Compose adaptive UI, or Compose theming/styles work, review the relevant local `SKILL.md` before implementing.

## Feature Test Policy

When implementing a feature, update tests at the same time as the production code:

- Unit tests: cover DTO/domain mapping, repository request/response behavior, UI state transformation, and view model state transitions.
- Robolectric Compose UI tests: cover user-visible screen behavior, callbacks, empty/loading/error states, and navigation events that do not require a real device.
- Screenshot tests: add or update Compose Preview Screenshot Testing baselines for changed screens, important states, and reusable feature UI components.
- Navigation tests: when adding or changing destinations, cover top-level destination mapping and back-stack behavior with pure Kotlin tests where possible.
- CI/reporting: keep `.github/workflows/android.yml` running `testDebugUnitTest`, `jacocoDebugUnitTestReport`, and `validateDebugScreenshotTest`, and keep publishing the HTML reports to GitHub Pages.

If a test category is not applicable, write the reason in the PR/Linear update. Avoid skipping a category silently.

For screenshot tests, use the GitHub Actions runner as the source of truth for baselines when local rendering differs from CI rendering. Screenshot reports should remain viewable from the published GitHub Pages report.

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
