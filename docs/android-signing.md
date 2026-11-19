# Shared Android signing

Android checks the application ID and signing certificate when installing an update. Both Macs and GitHub releases should sign `app.tinks.tink` with the same key. Use the keystore from the Mac that signed the existing install to keep updating it in place. For a build made with Android Studio's default debug signing, check `~/.android/debug.keystore` on that Mac.

## Check the certificate

Use the Android SDK's `apksigner` to compare an APK from that Mac with the APK installed on the phone. Pulling the installed APK reads the app package and leaves its data alone.

```sh
adb shell pm path app.tinks.tink
adb pull '<base.apk path from the command above, without package:>' /tmp/tink-installed.apk
apksigner verify --print-certs /tmp/tink-installed.apk
apksigner verify --print-certs /path/to/app-built-on-original-mac.apk
```

The signer certificate SHA-256 digests must match. If the original key is unavailable, a replacement key can sign future builds but Android will require a fresh installation to switch keys.

## Set up both Macs

Keep the shared keystore in a private local location, such as `~/.android/tink-shared.keystore`. Put these settings in the Android repo's ignored `local.properties` or supply them as environment variables. Use the full path on each Mac.

```properties
TINK_ANDROID_SIGNING_STORE_FILE=/Users/your-name/.android/tink-shared.keystore
TINK_ANDROID_SIGNING_STORE_PASSWORD=your-store-password
TINK_ANDROID_SIGNING_KEY_ALIAS=your-key-alias
TINK_ANDROID_SIGNING_KEY_PASSWORD=your-key-password
```

Run `./gradlew assembleDebug` to build `app/build/outputs/apk/debug/app-debug.apk`. Verify the certificate before using `adb install -r`. An unconfigured clone can still build with its machine's default debug key for disposable local installs.

## Store an encrypted copy in Git

Use GPG to encrypt the keystore with a strong passphrase held separately in a password manager. Commit only `signing/tink-shared.keystore.gpg`. Keep the keystore passwords out of Git as well.

```sh
mkdir -p signing
gpg --symmetric --cipher-algo AES256 \
  --output signing/tink-shared.keystore.gpg "$HOME/.android/tink-shared.keystore"
```

On the other Mac, clone or pull the repo and decrypt the file once.

```sh
umask 077
gpg --output "$HOME/.android/tink-shared.keystore" \
  --decrypt signing/tink-shared.keystore.gpg
```

## Sign GitHub releases

Set these GitHub Actions repository secrets using the same keystore and passwords as both Macs.

| Secret | Value |
| --- | --- |
| `TINK_ANDROID_SIGNING_STORE_BASE64` | Base64-encoded keystore file bytes |
| `TINK_ANDROID_SIGNING_STORE_PASSWORD` | Keystore password |
| `TINK_ANDROID_SIGNING_KEY_ALIAS` | Key alias |
| `TINK_ANDROID_SIGNING_KEY_PASSWORD` | Key password |

With GitHub CLI, load the keystore secret without printing its contents.

```sh
base64 < "$HOME/.android/tink-shared.keystore" | gh secret set TINK_ANDROID_SIGNING_STORE_BASE64
```

Use `gh secret set SECRET_NAME` to enter each remaining value interactively. The release workflow signs the APK used for Appium and published on GitHub with the shared key. Daily checks on `main` can run without the key.
