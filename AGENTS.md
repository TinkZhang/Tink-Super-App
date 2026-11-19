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
- Draft app UI or navigation changes in Figma before coding and get user approval on the linked frames. Keep the approved draft link with the feature notes in `docs/ui/`.
- Keep daily implementation and visual review tied to the approved draft. Refresh the Current UI frames and catalog from the verified Android release branch.

## Project Skills

Official Google Android skills are available under `.agent/skills/android/`. When a task matches edge-to-edge UI, testing setup, Navigation 3, Compose adaptive UI, or Compose theming/styles work, review the relevant local `SKILL.md` before implementing.

## Feature Tests

Update the relevant unit, Robolectric Compose UI, screenshot, and navigation tests with the production code. Cover mappings, repository behavior, view model transitions, visible screen states, and navigation as applicable. Record any inapplicable coverage in the completion summary. Appium mock and development API scenarios live in `tink-test`.

For daily verification, select relevant non-UI tests explicitly with `./gradlew testDebugUnitTest --tests '<test class>'`. The default CI on `main` runs selected non-UI checks and builds the debug APK. Run the full unit and UI suites on the release branch. Local UI runs or screenshot baseline updates require a separate explicit user request. Use GitHub Actions for Android verification when local Java is unavailable.

## Releases

- The user creates and pushes a branch named `release/vX.Y.Z` from the desired commit on `main`. Each release push runs the **Prepare Figma refresh** job alongside release verification. The Actions summary and `figma-refresh` artifact identify candidate frames and changed UI sources since the catalog’s reference revision.
- After reviewing the release candidate, use the authenticated Figma session to refresh the affected Current UI frames. Update the catalog’s reference revision to the verified release commit after completing the refresh.
- The Android workflow runs the debug and release unit suites, Robolectric UI tests, JaCoCo reporting, and Compose screenshot validation. It builds one debug APK and installs that exact artifact in the Appium mock and development API E2E suites from `tink-test`.
- Once all checks pass, the workflow publishes the matching GitHub release tag with notes from Git history, the tested debug APK, and a ZIP containing HTML test reports and logs. Failed runs retain their available reports in GitHub Actions.
- If a release check fails because of an app defect, fix and push it on `main`. The user can then update the release branch to the corrected commit. Use Actions to rerun transient failures on the same commit.
