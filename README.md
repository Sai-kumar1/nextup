# NextUp

A simple, offline todo list for Android, built with Kotlin, Jetpack Compose (Material 3) and Room.

**Features**
- **Plan by day:** the home screen shows only today's tasks. Swipe the week strip or open the calendar to plan any other day; dots mark days that have tasks. *Today* jumps back.
- **Tasks with notes:** tap a task to view it. Links in notes open in the browser on tap and are copied to the clipboard on long-press. Edit a task to change its title, notes or day.
- **Done and undo:** tick tasks off (they move to a collapsible *Completed* section), swipe to delete with Undo, or clear a day's completed tasks.
- **Light and dark themes:** the sun/moon button switches themes with a circular reveal (light rises from the bottom, dark falls from the top). It follows the system theme until you use it. Colors match [sai-kumar1.github.io](https://sai-kumar1.github.io): a soft gray canvas with white cards in light, layered dark grays in dark (no pure white or black), and a crimson accent.
- **Friendly empty states:** an animated ☕ when nothing is planned, and a 🎉 when every task of the day is done.
- Everything is stored on the device.

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

The release workflow signs the APK with a keystore stored as repository secrets. This is already set up for this repo; the steps below are for recreating it.

1. Create a PKCS12 keystore with OpenSSL (no JDK needed):
   ```sh
   mkdir -p ~/nextup-signing && cd ~/nextup-signing && umask 077
   PASS=$(openssl rand -hex 24)
   openssl req -x509 -newkey rsa:4096 -sha256 -days 10000 -nodes \
     -subj "/CN=NextUp" -keyout key.pem -out cert.pem
   openssl pkcs12 -export -name nextup -inkey key.pem -in cert.pem \
     -out nextup-release.p12 -passout pass:"$PASS"
   rm key.pem
   printf 'KEYSTORE_PASSWORD=%s\nKEY_PASSWORD=%s\nKEY_ALIAS=nextup\n' "$PASS" "$PASS" > secrets.env
   ```
   (`keytool -genkeypair -keystore nextup-release.jks -alias nextup -keyalg RSA -keysize 4096 -validity 10000` from a JDK works too.)
2. Upload the secrets with the GitHub CLI:
   ```sh
   gh secret set KEYSTORE_BASE64 < <(base64 -w0 nextup-release.p12)
   grep '^KEYSTORE_PASSWORD=' secrets.env | cut -d= -f2- | tr -d '\n' | gh secret set KEYSTORE_PASSWORD
   grep '^KEY_PASSWORD=' secrets.env | cut -d= -f2- | tr -d '\n' | gh secret set KEY_PASSWORD
   printf nextup | gh secret set KEY_ALIAS
   ```
   Or add them by hand under **Settings → Secrets and variables → Actions**.

> **Back up `~/nextup-signing/` (keystore and `secrets.env`) somewhere safe, and never commit it** (`*.jks`, `*.p12` and `*.keystore` are git-ignored). Android only installs an update over an existing app if both are signed with the same key.

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

## Credits

Animated emoji from [Google Noto Emoji](https://googlefonts.github.io/noto-emoji-animation/), licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).
