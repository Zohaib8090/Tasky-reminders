# Build and release

## Requirements
- JDK 17 or newer
- Android SDK with platform 36
- Gradle is provided by the wrapper (`./gradlew`)

## Local builds
```bash
./gradlew assembleDebug     # app/build/outputs/apk/debug/
./gradlew test              # unit tests
```
Local builds use version `1.0` / code `1`. A debug keystore (`debug.keystore`) is expected in the repo root, and is git-ignored.

## Versioning
Gradle reads `-PappVersionName` and `-PappVersionCode`. CI sets them: for a tag `vX.Y.Z` the name is `X.Y.Z` and the code is `X*10000 + Y*100 + Z`. Other CI builds use `1.0-dev.<run number>`. Version codes must increase with every release, or Android refuses the update.

## CI (`.github/workflows/build-and-release.yml`)
| Trigger | Result |
|---|---|
| Push or PR to `main` | Debug APK as the `TaskNest-APKs` artifact (30 days) |
| Manual run, choose debug / release / both | Chosen APKs as an artifact; release needs the secrets |
| Push of a tag `v*` | Signed release APK as an artifact and attached to a GitHub Release |

Without the release secrets, a tag build falls back to a debug APK, which cannot update a release install.

## One-time signing setup
```bash
keytool -genkeypair -v -keystore my-upload-key.jks -alias upload \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 my-upload-key.jks
```
Add repository secrets `KEYSTORE_BASE64` (the base64 output), `STORE_PASSWORD` and `KEY_PASSWORD`. Newer Java keystores use one password for both, so set the two secrets to the same value. Back up the `.jks` file; losing it means existing users must reinstall.

## Publishing a version
1. Make sure `main` builds green.
2. Create a release on GitHub with a new tag such as `v1.1.0` on `main` (or push the tag from a computer).
3. The workflow builds the signed APK and attaches it to the release.
4. Users see it under Settings, App Updates, Check for updates.

## Changing the package name
`applicationId` in `app/build.gradle.kts` is the app's identity. Changing it makes Android treat the app as a different one, so existing users must reinstall and restore from a backup.

## Database changes
Add a Room `Migration` and bump the version in `AppDatabase` for every schema change. Without it, the destructive fallback wipes users' data.
