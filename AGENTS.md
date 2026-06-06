# AGENTS.md — Han1meViewer

Compact pointer for OpenCode sessions. Deep context lives in `CLAUDE.md`, `QWEN.md`, and `README_TECH.md` — read those when you need detail on a subsystem.

## Project
Android R18 client. Kotlin 2.3.21 + Jetpack Compose, AGP 9.2.1, JDK 21, minSdk 27 / target 37. Fork of [YenalyLiew/Han1meViewer](https://github.com/YenalyLiew/Han1meViewer), Apache 2.0. **No public promotion.** Data is fetched via standard HTTP + Jsoup HTML parsing only — no backend access. GitHub Releases + CI artifacts are the only distribution channels.

## Build & verify

User shell is **PowerShell on Windows** — use `.\gradlew.bat`, not `./gradlew`.

- `.\gradlew.bat :app:compileDebugKotlin` — required gate before opening a PR
- `.\gradlew.bat :app:assembleDebug` — debug APK (`com.wuwei.han1meviewer.debug`)
- `.\gradlew.bat :app:lintDebug` / `:app:lint` — Android Lint (suppresses `EnsureInitializerMetadata`)
- `.\gradlew.bat :app:testDebugUnitTest` — JUnit (note: PR CI does **not** run unit tests; see CI below)
- `.\gradlew.bat :app:connectedDebugAndroidTest` — requires device/emulator
- After adding/changing Room entities/DAOs: `.\gradlew.bat :app:clean build` (KSP must regenerate)
- `./gradlew dependencyUpdates` — version-catalog check

## CI gates

- `pr_check.yml` → `./gradlew buildDebug -x lint` only. No unit tests, no lint. If your change breaks the debug build, CI catches it; otherwise CI is silent on functional regressions.
- `ci.yml` (push to `main`) → signed `assembleRelease` (arm64-v8a only). Decodes `app/google-services.json` from `GOOGLE_SERVICES_JSON_BASE64` and the keystore from `YOUR_KEYSTORE_BASE64` into `$HOME/.android/keystore.jks`. Sets `HA1_VERSION_SOURCE=ci`.

## Local prerequisites (often missing on fresh clones)

- `app/google-services.json` is **gitignored**. Local debug builds will fail without it. Obtain a Firebase config file matching package `com.wuwei.han1meviewer` (debug also has `.debug` suffix).
- `app/ha1_github_token.txt` is gitignored. Without it, `BuildConfig.HA_GITHUB_TOKEN` is empty and release update checks fail. CI provides `HA_GITHUB_TOKEN` env var.
- Release signing: env vars `KEYSTORE_PASSWORD` (reused for both store and key password — see `app/build.gradle.kts:59`) and `KEY_ALIAS`. Keystore path: `$HOME/.android/keystore.jks`. **Debug builds need none of this.**

## Architecture (one-screen)

- Modules: `:app` (everything), `:yenaly_libs` (shared base classes; resourcePrefix `yenaly_`, namespace `com.yenaly.yenaly_libs`), `buildSrc` (versioning in `Config.kt`).
- Data flow: `Compose → ViewModel → NetworkRepo / DatabaseRepo → Retrofit+Jsoup / Room (KSP) → StateFlow → Compose`.
- State wrappers: `WebsiteState` (non-paged), `PageLoadingState` (paged lists), `VideoLoadingState` (video detail) — under `logic/state/`.
- Network: Retrofit 3 + OkHttp 5 with Cloudflare / UA / speed-limit / DoH interceptors under `logic/network/`. Mirror domains via `Preferences.baseUrl` (`hanime1.me`, `hanime1.com`, `hanimeone.me`, `javchu.com`).
- Navigation: typed Kotlin Serialization routes under `ui/navigation/main` and `ui/navigation/settings`; use `navController.navigateSafely(...)` (raw `navigate` will double-push on rapid taps).
- Players (3): JZVD `HJzvdStd`, Media3 ExoPlayer, MPV Android — selected via `HMediaKernel` user preference.
- Downloads: `HanimeDownloadManagerV2` → WorkManager `HanimeDownloadWorker` → Room (many-to-many: `HanimeDownloadEntity` ↔ `DownloadCategoryEntity` via `HanimeCategoryCrossRef`).
- Parser layer (`logic/Parser.kt`) must not touch DB. When the target site DOM changes, fix the parser first.

## Conventions (enforced by existing code)

- `LazyColumn` / `LazyRow` / `LazyVerticalGrid` items **must have stable unique keys**. Paginated lists in `*ViewModel` use `(previous + incoming).distinctBy(<stableKeyField>)` before exposing StateFlow (e.g. `HanimeInfo::videoCode`, `Playlists.Playlist::listCode`). Duplicate keys crash with `IllegalArgumentException: Key "..." was already used`.
- Network/DB access must not be called from Composable; route through ViewModel.
- Page state via `StateFlow`; one-shot events via `SharedFlow` or callbacks.
- `NetworkRepo` must propagate `CancellationException` (don't wrap in generic catch). Map Cloudflare / IP-blocked / login-expired / not-found to typed exceptions under `logic/exception/` at the repo or parser layer.
- New UI = Compose. XML/ViewBinding remains only for login, Cloudflare, a few historical pages.
- Text files: CRLF line endings, trailing newline.
- Code style: `kotlin.code.style=official` (`gradle.properties:19`).

## Frequently-missed gotchas

- Debug `applicationIdSuffix = ".debug"` → debug and release are different package ids. Firebase config must register both.
- ABI split: `isEnable = taskRequests.toString().contains("Release")` (`app/build.gradle.kts:65`). `assembleRelease` emits only `arm64-v8a`; debug emits universal. Don't expect multi-ABI release artifacts.
- `Config.lastCommitSha` shells out to `git rev-parse`. A non-git source tree will break Gradle config evaluation. CI is fine; bare tarballs aren't.
- `HanimeApplication` + `HInitializer` set up Firebase, MPV, launcher-icon aliases (default / fake calc / fake Cornhub / fake Xxt), and the global `UncaughtExceptionHandler` (`HCrashHandler`). Compile alone won't catch NPEs in init paths — needs a real device.
- Test source sets contain only one stub `ExampleInstrumentedTest`. No real unit tests. Adding tests is fine but CI will not run them.
- Versioning: `Config.createVersion(major, minor, patch)` returns `(versionCode, versionName)`. `source` = `debug` if task name lacks "Release" (or `HA1_VERSION_SOURCE=debug`), else `release` (or env). Non-debug `versionCode` = UTC `yyMMddHH`.

## Where to start editing

| Change | Primary files |
|---|---|
| New home module | `ui/screen/home/HomePageScreen.kt`, `ui/viewmodel/MainViewModel.kt`, `logic/Parser.kt` (`homePageVer2`), `logic/model/HomePage*.kt` |
| New search filter | `ui/screen/search/SearchScreen.kt`, `AdvancedSearchSheet.kt`, `ui/viewmodel/SearchViewModel.kt`, `HAdvancedSearch.kt`, `logic/NetworkRepo.kt` (`getHanimeSearchResult`), `logic/network/service/HanimeBaseService.kt` |
| New video detail field | `logic/model/HanimeVideo.kt`, `logic/Parser.kt` (`hanimeVideoVer2`), `ui/screen/video/VideoIntroductionScreen.kt` |
| New "my" list page | `ui/navigation/main/MainRoutes.kt`, `MainNavHost.kt`, RouteScreen, ViewModel, `NetworkRepo`, `Parser` |
| New download capability | `ui/viewmodel/DownloadViewModel.kt`, `worker/HanimeDownloadManagerV2.kt`, `HanimeDownloadWorker.kt`, `logic/dao/DownloadDatabase.kt` |
| New setting | `Preferences.kt` (or `*Config.kt`), `ui/screen/settings/*Screen.kt`, then update the actual reader |
| DOM change on target site | start at `logic/Parser.kt` |
| Shared keyframes data | `app/src/main/assets/h_keyframes/`, PR template at `.github/PULL_REQUEST_TEMPLATE/submit_h_keyframe.md` |

## Other repo content

- `HanimeAnnouncementManagerWebUI/` — standalone HTML + Python tooling for the Firebase Realtime Database `announcements` node. Not built with Gradle. Keep its changes out of `:app` PRs.

## Related instruction files in this repo

- `CLAUDE.md` — English project overview.
- `QWEN.md` — Chinese project context, mid-detail, more file-level pointers.
- `README_TECH.md` — Chinese technical deep-dive per subsystem (parsers, downloads, announcements, keyframes, settings, etc.). Best reference when changing one subsystem in depth.
- `README.md` — user-facing disclaimer + announcement tooling usage. The "no public promotion" rule lives here.
