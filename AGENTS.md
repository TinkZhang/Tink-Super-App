# Repository Instructions

`Tink-Super-App` is the Android client for Tink, built with Kotlin and Jetpack Compose.

## Daily Development

- Work directly on `main`. After each Codex request that changes files, commit the request's changes and push to `origin/main` before reporting completion.
- Sync with `origin/main` before starting work. Preserve unrelated changes and use a clean checkout of `main` if the existing checkout contains work in progress.
- Keep Retrofit interfaces aligned with `api-contract/dist/openapi.yaml`.
- Follow existing Compose, Hilt, repository, and view model patterns.
- Keep API models close to the feature package unless an existing shared model is already used.
- Keep local Android Studio files, build outputs, keystores, and secrets out of Git.

## UI Design

- Use the [UI catalog](docs/ui/README.md) and existing Compose components to understand the current UI. Follow Material 3 and the app's dynamic color theme.
- Draft app UI or navigation changes as offline HTML in `docs/ui/drafts/<feature>/index.html` and get user approval before coding, unless asked to proceed directly. Link the approved file in the feature notes.
- Validate the implemented UI on the native app or through Compose previews. Use the release HTML gallery as the record of the shipped interface.

## Project Skills

Official Google Android skills are available under `.agent/skills/android/`. When a task matches edge-to-edge UI, testing setup, Navigation 3, Compose adaptive UI, or Compose theming/styles work, review the relevant local `SKILL.md` before implementing.

## Feature Tests

Update the relevant unit, Robolectric Compose UI, screenshot, and navigation tests with the production code. Cover mappings, repository behavior, view model transitions, visible screen states, and navigation as applicable. Record any inapplicable coverage in the completion summary. Appium mock and development API scenarios live in `tink-test`.

For daily verification, select relevant non-UI tests explicitly with `./gradlew testDebugUnitTest --tests '<test class>'`. The default CI on `main` runs selected non-UI checks and builds the debug APK. Run the full unit and UI suites on the release branch. Local UI runs or screenshot baseline updates require a separate explicit user request. Use GitHub Actions for Android verification when local Java is unavailable.

## Releases

- The user creates and pushes `release/vX.Y.Z` from the desired commit on `main`. CI renders and packages an offline HTML UI gallery after screenshot validation. Use native screenshots of affected states to supplement it where automated previews lack coverage.
- The Android workflow runs the debug and release unit suites, Robolectric UI tests, JaCoCo reporting, and Compose screenshot validation. It builds one debug APK and installs that exact artifact in the Appium mock and development API E2E suites from `tink-test`.
- Once all checks pass, the workflow publishes the matching GitHub release tag with notes from Git history, the tested debug APK, a ZIP containing HTML test reports and logs, and `android-ui-gallery.zip`. Failed runs retain their available reports in GitHub Actions.
- If a release check fails because of an app defect, fix and push it on `main`. The user can then update the release branch to the corrected commit. Use Actions to rerun transient failures on the same commit.
