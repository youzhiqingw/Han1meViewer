# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Project

**Han1meViewer** — Android R18 client (min SDK 27, target SDK 37) for browsing hanime content. Kotlin + Jetpack Compose. Forked from YenalyLiew/Han1meViewer, Apache 2.0. **No public promotion allowed.** Data fetched via standard HTTP + HTML DOM parsing only; no backend access.

## Build Commands

```bash
# Compile debug (required before submitting)
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug

# Windows PowerShell
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:assembleDebug

# Lint (Android Lint)
./gradlew :app:lint
./gradlew :app:lintDebug

# Unit tests
./gradlew :app:testDebugUnitTest

# Instrumentation tests (requires emulator/device)
./gradlew :app:connectedDebugAndroidTest

# Dependency version check
./gradlew dependencyUpdates

# Start from a clean state
./gradlew clean
```

## Environment Requirements

- JDK 21, Android Studio Panda or newer
- Gradle Wrapper handles Gradle version automatically
- **`HA_GITHUB_TOKEN`** env var (or `app/ha1_github_token.txt` file) for update checks
- **Release signing**: `KEYSTORE_PASSWORD` + `KEY_ALIAS` env vars only; debug builds use `.debug` applicationId suffix and are unsigned
- CI decodes `google-services.json` from base64 GitHub Secret at build time
- **All PRs must pass `:app:compileDebugKotlin`** before being submitted

## Compiler / Tooling Notes

- Kotlin 2.3.x, Java 21 toolchain, AGP 9.2.x
- KSP for Room annotation processing; run `./gradlew :app:clean build` after adding/changing entities
- `buildSrc/src/main/java/Config.kt` — versioning logic (`Config.Version`, commit SHA via `git rev-parse`)

## Architecture

### Data Flow

```
Compose Screen -> ViewModel -> NetworkRepo / DatabaseRepo -> Retrofit + Jsoup / Room (KSP) -> StateFlow -> Compose Screen
```

### Specific Data Flows

- **Video page**: `VideoRoute -> VideoViewModel -> NetworkRepo.getHanimeVideo -> Parser::hanimeVideoVer2 -> HanimeVideo -> VideoScreen / Player / CommentScreen`
- **Download**: `DownloadScreen -> DownloadViewModel -> HanimeDownloadManagerV2 -> WorkManager -> Room -> Flow -> UI`

### Modules

- `:app` — all business logic, UI, network, database, workers
- `:yenaly_libs` — shared base classes (`YenalyActivity`, `YenalyFragment`, `YenalyViewModel`), utility classes (ClipboardUtil, SharedPreferencesUtil, ImageUtil, LanguageHelper, etc.); resource prefix `yenaly_`
- `buildSrc` — versioning, build type detection, commit SHA extraction

## Key Source Layout (`com.wuwei.han1meviewer`)

- **`Constants.kt` + `Preferences.kt`** — global constants, SharedPreferences wrappers with `StateFlow` for login/cookie state
- **`HanimeApplication.kt`** — Firebase init, MPV init, custom launcher icon switching, global proxy setup
- **`HInitializer.kt`** — sets global `UncaughtExceptionHandler` (`HCrashHandler`) via `YenalyInitializer`
- **`NetworkRepo.kt`** — singleton; wraps all Retrofit + Jsoup calls into typed `flow` wrappers (`websiteIOFlow`, `pageIOFlow`, `videoIOFlow`) — pattern: `request = { service.call() }`, `action = Parser::method`
- **`DatabaseRepo.kt`** — Room DAO façade for all local data
- **`logic/model/`** — data classes for parsed responses (`HanimeInfo`, `HanimeVideo`, `HomePage`, `VideoComments`, `SearchOption`, etc.)
- **`logic/network/`** — Retrofit services, OkHttp interceptors (Cloudflare, speed limit, UA), DNS-over-HTTPS, proxy config
- **`logic/dao/` + `logic/entity/`** — Room databases and entities: downloads (many-to-many with categories), watch history, search history, keyframes, check-in records, side dishes
- **`worker/`** — `HanimeDownloadManagerV2` (WorkManager download pipeline), `HUpdateWorker` (app updates)
- **`util/`** — theme, network utilities, file management (`SafFileManager`), permissions, cookie parsing (`CookieString`), video utilities, MPV shader/cert asset copying

## Navigation

- Two-layer NavHost: `MainNavHost` (home, search, video, download, account, settings) + `SettingsNavHost` (sub-settings pages)
- Routes defined per-feature under `ui/navigation/main/` and `ui/navigation/settings/`
- Uses `navigateSafely` extension + Kotlin Serialization typed route classes (`toRoute<T>()`)

## ViewModel Conventions

- `StateFlow` exposes page state; `SharedFlow` or callbacks for one-shot events
- ViewModels: `HomeViewModel`, `SearchViewModel`, `VideoViewModel`, `DownloadViewModel`, `MainViewModel`, `SettingsViewModel`, `UserAccountViewModel`, `MyListViewModel`, `MySubscriptionsViewModel`, `CreatorCenterViewModel`, `PreviewViewModel`, `CheckInCalendarViewModel`, etc.
- Each `ui/screen/<feature>/` folder contains composables + sub-packages

## Playback Architecture

- Three kernels: **JZVD** (`HJzvdStd`), **Media3 ExoPlayer**, **MPV Android** (`MPVLib`) — selected via `HMediaKernel` interface, user preference

## Network & Parsing

- All HTML parsed via Jsoup in `Parser.kt` — Parser layer never touches DB
- Cloudflare challenges: `CloudflareInterceptor` + `CloudflareActivity` (WebView-based)
- Cookies: `HCookieJar` backed by SharedPreferences string; mirrored to `Preferences.loginCookie` / `Preferences.cloudFlareCookie` StateFlows
- Mirror domains configurable: `hanime1.me`, `hanime1.com`, `hanimeone.me`, `javchu.com` via `Preferences.baseUrl`

## Special Systems

- **Announcements** — read from Firebase Realtime Database (`announcements` node) by `MainViewModel.loadAnnouncements()`; managed via separate `HanimeAnnouncementManagerWebUI` HTML/Python tooling; only `isActive=true` shown, prioritized by `priority`, 24h dismiss cooldown
- **Check-in** — `CheckInRecordDatabase` with `CheckInType` enum; daily widget support
- **Keyframes** — `HKeyframeDao` for persisting key H-frame timestamps; shared with community via `SharedHKeyframesRoute`; PR template at `.github/PULL_REQUEST_TEMPLATE/submit_h_keyframe.md`
- **Privacy** — app lock (biometric), launcher icon disguise (4 aliases: default / fake calc / fake Cornhub / fake Xxt), manual cookie management

## Important Patterns

- `LazyColumn`/`LazyVerticalGrid`/`LazyRow` items **must use stable unique keys** — duplicate keys cause Compose rendering corruption
- Mixed XML/ViewBinding (login, cloudflare) + Compose — new features should be Compose
- `MultiItemEntity` interface for heterogeneous RecyclerView lists (BaseRecyclerViewAdapterHelper4 / MultiType)
- Download many-to-many: `HanimeDownloadEntity` ↔ `DownloadCategoryEntity` via `HanimeCategoryCrossRef`

## CI & Release

- CI: `ci.yml` builds signed Release APK (arm64-v8a only), uploads as artifact
- GitHub releases + CI artifacts are the **only** distribution channels

## Contacts & Disclaimers

No public promotion allowed. Data fetched via standard HTTP + HTML DOM parsing only; no backend access. See README.md for full legal disclaimer. Telegram: https://t.me/Han1meViewer