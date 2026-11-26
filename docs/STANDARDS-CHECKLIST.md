# app-standards checklist (status of this project)

| Item | Status |
|---|---|
| Stack | Native Kotlin + Jetpack Compose (the skill's exception: Android-TV-only app) |
| App icon + TV banner | Placeholder crescent (vector). Replace with the final design. Banner has no text yet. |
| Diagonal version ribbon (bottom corner, RTL-aware) | Done (`VersionRibbon.kt`) |
| Splash with icon, progress bar, % counter (real steps) | Done (`Root.kt`) |
| Light/dark | Done via presets (Emerald, Night, Black & Gold, Light) + fully custom colors |
| ar (RTL) / en / de, per-app language in Android settings | Done (`locales_config.xml`, AppCompat locales, in-app picker) |
| Update check at start, SHA-256, install over old version | Done (`Updater.kt`, CI creates `latest.json`) |
| Update modes | Ask / auto-download then ask; forced via `mandatory`/`minBuild` in latest.json |
| Same signature, no data loss | Needs your keystore: `scripts/create_keystore.sh` + 4 GitHub secrets |
| Error log, rotating, redacted, LAN browser viewer | Done (`AppLog.kt`, `LanServer.kt`); viewer off by default |
| Privacy / GDPR | No data collected; `PRIVACY.md` draft; Impressum needed before commercial use |
| Tests + CI | Unit tests (parser, updater, redaction, next prayer); CI builds on every push, release on tags |
| Settings screen | Done (TV panel + phone page generated from one schema) |
| Bundled fonts (Cairo/Inter) | NOT yet: the system font is used. Add font files to `res/font` later. |
| Golden/UI tests | NOT yet |
| TV focus / D-pad | Done in settings panel; main screen is display-only |
