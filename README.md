# NextUp

A simple, offline todo list for Android, built with Kotlin, Jetpack Compose (Material 3) and Room.

**Features:** add, edit and delete tasks, mark them done (they move to a collapsible *Completed* section), swipe to delete with Undo, clear all completed tasks, and light/dark themes with Material You colors. Everything is stored on the device.

## Getting the APK

APKs are built by GitHub Actions; no local Android toolchain is needed.

| What | When | Where |
|---|---|---|
| Debug APK | every push and pull request | Actions → *Android CI* run → **Artifacts** → `nextup-debug-<sha>` (a zip containing the APK) |
| Signed release APK | when you push a tag `v*` | **Releases** page → `nextup-<version>.apk` |

The debug build uses the application id `io.github.saikumar1.nextup.debug`, so it can be installed next to the release build.

To install, copy the APK to your phone and open it (allow "install unknown apps" when asked), or run `adb install nextup-x.y.z.apk`.

## Making a release

```sh
git tag v0.1.0
git push origin v0.1.0
```

The tag sets `versionName` (`v0.1.0` → `0.1.0`). `versionCode` is the workflow run number, so it always increases.

## Release signing (one-time setup)

The release workflow needs a keystore stored as repository secrets.

1. Generate a keystore (`keytool` comes with any JDK, e.g. `sudo apt install openjdk-17-jdk-headless`):
   ```sh
   keytool -genkeypair -v -keystore nextup-release.jks -alias nextup \
     -keyalg RSA -keysize 4096 -validity 10000
   ```
2. Encode it:
   ```sh
   base64 -w0 nextup-release.jks > nextup-release.jks.b64
   ```
3. In GitHub, open **Settings → Secrets and variables → Actions** and add:

   | Secret | Value |
   |---|---|
   | `KEYSTORE_BASE64` | contents of `nextup-release.jks.b64` |
   | `KEYSTORE_PASSWORD` | the keystore password |
   | `KEY_ALIAS` | `nextup` (or the alias you chose) |
   | `KEY_PASSWORD` | the key password (same as the keystore password if you pressed Enter) |

   Or use the GitHub CLI: `gh secret set KEYSTORE_BASE64 < nextup-release.jks.b64`, and so on.

> **Back up `nextup-release.jks` and its passwords somewhere safe, and never commit it** (`*.jks` is git-ignored). Android only installs an update over an existing app if both are signed with the same key.

## Building locally (optional)

This needs JDK 17 and the Android SDK (`ANDROID_HOME` set, or a `local.properties` file with `sdk.dir=...`):

```sh
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleDebug       # app/build/outputs/apk/debug/
```

## Project layout

```
app/src/main/java/io/github/saikumar1/nextup/
  data/   Task entity, Room DAO/database, TaskRepository
  ui/     TaskViewModel, TaskScreen (Compose), theme
.github/workflows/
  android-ci.yml   tests + lint + debug APK on every push/PR
  release.yml      signed release APK on v* tags
```
