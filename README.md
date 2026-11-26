# Mawaqit Alathan — مواقيت الأذان

A prayer-times screen for **Google TV, Android TV and Amazon Fire TV** (Fire OS 6 / Android 7+). The whole screen is drawn by the app and every part is adjustable: layout, sizes, colors, spacing, corner radius, digits, language and more. Prayer times come from your mosque page on [mawaqit.net](https://mawaqit.net).

Built with the personal `app-standards` skill (splash, version ribbon, signed in-app updates, log viewer, ar/en/de, GDPR-friendly).

## Use it
1. Install the APK from the latest **Release**.
2. Press **OK** or **Menu** on the remote to open settings.
3. Paste your mosque page, e.g. `https://mawaqit.net/de/your-mosque`. Without it the app calculates times for the city you enter (Aladhan API).
4. Change anything live; the screen on the left updates while you adjust. Left/Right change a value, OK toggles.
5. **Control from your phone:** turn on *Phone control & log viewer (LAN)*, open the shown address in a browser on the same Wi-Fi. All settings and the live log are there.

> mawaqit.net has no public API for this. The app reads the data embedded in the mosque page; if the site changes, the app falls back to calculated times (and the log shows why).

## Build
Open in Android Studio (it creates the Gradle wrapper), or `gradle assembleDebug` with Gradle 8.10+ and JDK 17. Debug builds run without any key.

## Release and updates (one-time setup)
1. On your PC: `scripts/create_keystore.sh Mawaqit-Alathan` (Windows: `scripts/create_keystore.ps1 -App Mawaqit-Alathan`). It makes the key and an encrypted backup. **Keep the backup in two places. If the key is lost, installed copies can never be updated.**
2. GitHub → Settings → Secrets → Actions: `KEYSTORE_B64` (content of the `.b64` file), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
3. Tag a release: `git tag v1.0.0 && git push --tags`. CI builds the signed APK + `latest.json` and creates a **draft** release; publish it.
4. The app checks `releases/latest/download/latest.json` at start and offers the update. It installs over the old version, data kept (needs "install unknown apps" allowed once).

The repository must be public for the updater to read releases without a token.

## Languages
العربية (RTL) · English · Deutsch. Android 13+: per-app language in system settings; also in the app's settings.

## Privacy
No data is collected. See [PRIVACY.md](PRIVACY.md). Standards status: [docs/STANDARDS-CHECKLIST.md](docs/STANDARDS-CHECKLIST.md).
