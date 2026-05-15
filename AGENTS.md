# Repository Instructions

`Tink-Super-App` is the Android client for Tink, built with Kotlin and Jetpack Compose.

## Rules

- Keep Retrofit interfaces aligned with `api-contract/dist/openapi.yaml`.
- Follow existing Compose, Hilt, repository, and view model patterns.
- Keep API models close to the feature package unless an existing shared model is already used.
- Do not commit local Android Studio files, build outputs, keystores, or secrets.

## Verification

Run when Java is available:

```sh
./gradlew testDebugUnitTest
```

GitHub Actions installs Java and is the authoritative Android gate when local Java is unavailable.
