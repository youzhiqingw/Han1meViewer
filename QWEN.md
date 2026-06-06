# QWEN.md — Han1meViewer 项目上下文

## 项目概述

Han1meViewer 是一个 Kotlin 开发的 Android 客户端应用，用于浏览、搜索、播放和管理 hanime1.me 站的公开视频页面内容。项目最初 Fork 自 YenalyLiew/Han1meViewer，继承 Apache License 2.0。

**核心技术栈：** Kotlin 2.3.x · Java 21 toolchain · Android Gradle Plugin 9.2.x · Jetpack Compose + Material 3 · Navigation Compose (typed routes via Kotlin Serialization) · ViewModel + StateFlow + SharedFlow · Retrofit 3 + kotlinx.serialization converter · Jsoup HTML 解析 · Room + KSP · WorkManager · Coil 3 · Media3 ExoPlayer / JZVD / MPV Android · Firebase (Analytics, Crashlytics, Performance, Remote Config, Realtime Database)

**构建目标：** compileSdk=37, minSdk=27, targetSdk=37, Java 21, Kotlin JVM target 21

## 项目结构

```text
Han1meViewer/
├── app/                              主 Android 应用模块
│   └── src/main/java/com/yenaly/han1meviewer/
│       ├── logic/                     数据层
│       │   ├── network/               Retrofit Service、OkHttp、DNS、Cookie、拦截器
│       │   │   ├── service/           HanimeBaseService, HanimeMyListService, HanimeCommentService, HanimeSubscriptionService, HGitHubService
│       │   │   ├── HanimeNetwork.kt   Retrofit Service 聚合
│       │   │   ├── ServiceCreator.kt  OkHttp/Retrofit 创建
│       │   │   ├── HCookieJar.kt      Cookie 管理
│       │   │   ├── HDns.kt / GitHubDns.kt  自定义 DNS
│       │   │   ├── HProxySelector.kt  代理选择
│       │   │   └── interceptor/       Cloudflare、UA、限速、日志拦截器
│       │   ├── model/                 业务模型 (HanimeVideo, HanimeInfo, HomePage, Playlists 等)
│       │   ├── dao/                   Room DAO 和 Database (History, Download, Miscellany, CheckIn)
│       │   ├── entity/                Room 实体和本地实体
│       │   ├── state/                 WebsiteState, PageLoadingState, VideoLoadingState
│       │   ├── exception/             CloudFlareBlockedException, IPBlockedException, HanimeNotFoundException 等
│       │   ├── NetworkRepo.kt         网络仓库入口
│       │   ├── DatabaseRepo.kt        本地仓库入口
│       │   └── Parser.kt              HTML/JSON 解析入口
│       ├── ui/                        UI 层
│       │   ├── activity/              MainActivity, 登录, Cloudflare, 手动 Cookie 页面
│       │   ├── navigation/            Navigation Compose 路由定义和导航 Host
│       │   ├── screen/                Compose 页面 (home, search, video, settings, account, login 等)
│       │   ├── component/             可复用 Compose 组件 (视频卡片、弹窗、评论卡片等)
│       │   ├── viewmodel/             页面 ViewModel
│       │   ├── view/                  自定义 View 和播放器 View (JZVD, Media3)
│       │   ├── theme/                 Compose 主题、颜色、尺寸
│       │   ├── adapter/               View 系统列表适配器
│       │   ├── bridge/                View 与 Compose 桥接
│       │   ├── widget/                桌面小组件
│       │   ├── model/                 UI 侧模型
│       │   └── preview/               Compose Preview
│       ├── util/                       工具类 (文件、网络、权限、Cookie、Toast、视频等)
│       ├── worker/                     WorkManager 任务 (下载 Worker, 更新 Worker, 下载管理器)
│       ├── Preferences.kt             偏好设置入口
│       ├── HanimeApplication.kt       应用入口
│       ├── Constants.kt / FirebaseConstants.kt  常量
│       └── HanimeResolution.kt        画质解析
│   └── src/main/res/                  资源文件、主题、布局、图标
│   └── src/main/assets/h_keyframes/   共享关键 H 帧 JSON 数据
├── yenaly_libs/                        项目内公共基础库 (通用 Activity, Fragment, ViewModel, Preference, 工具类)
├── buildSrc/                           Gradle 构建辅助 (Config.kt: 版本号、构建来源、commit SHA)
├── HanimeAnnouncementManagerWebUI/     公告管理 Web 端 (HTML + Python)
├── gradle/libs.versions.toml           依赖版本目录
├── settings.gradle.kts                 Gradle 模块配置 (:app, :yenaly_libs)
└── build.gradle.kts                    根构建文件 (插件声明)
```

## 构建与运行

### 环境要求

- Android Studio Panda 或更新版本
- JDK 21
- Gradle Wrapper 使用仓库内置版本

### 常用命令

```bash
# 编译验证（提交前必须通过）
./gradlew :app:compileDebugKotlin

# 构建 Debug APK
./gradlew :app:assembleDebug

# Windows PowerShell
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:assembleDebug
```

### Release 构建

Release 构建需要签名环境变量：
- `KEYSTORE_PASSWORD` — keystore 密码
- `KEY_ALIAS` — key 别名

GitHub API Token 可通过：
- 环境变量 `HA_GITHUB_TOKEN`
- 本地文件 `app/ha1_github_token.txt`

### 版本管理

版本号在 `app/build.gradle.kts` 的 `defaultConfig` 中通过 `Config.Version.createVersion(major, minor, patch)` 生成。当前版本为 0.26.0。

版本来源 (`source`) 由 `HA1_VERSION_SOURCE` 环境变量或构建任务名决定：
- 包含 "Release" → `release`
- 否则 → `debug`

Release 版本号格式：`0.26.0-release+yyMMddHH`（UTC 时间戳作为 versionCode）
Debug 版本号格式：`debug+1`（固定 versionCode=1）

APK 输出文件名：`Han1meViewer-v{versionName}.apk`

Release 默认：混淆 + 资源压缩 + 仅 `arm64-v8a` ABI split。

## 数据流架构

整体采用 MVVM 模式：

```text
Compose Screen → ViewModel → NetworkRepo / DatabaseRepo → Retrofit + Jsoup / Room → StateFlow → Compose Screen
```

**网络数据流：**
```text
ViewModel → NetworkRepo → HanimeNetwork Service → Parser → WebsiteState/PageLoadingState/VideoLoadingState → ViewModel
```

**本地数据流：**
```text
ViewModel/Worker → DatabaseRepo → Room DAO → Flow / suspend result → ViewModel/UI
```

**下载数据流：**
```text
DownloadScreen → DownloadViewModel → HanimeDownloadManagerV2 → WorkManager Worker → Room → Flow → UI
```

## 状态模型

项目使用三个主要状态包装类型：
- `WebsiteState<T>` — 普通页面/操作状态 (Loading, Success, Error)，用于首页、订阅、账号等非分页数据
- `PageLoadingState<T>` — 分页列表状态 (Loading, Success, NoMoreData, Error)，用于搜索、收藏、稍后观看、播放列表等
- `VideoLoadingState<T>` — 视频详情状态，处理视频不存在、解析失败、加载中和成功状态

UI 层不应根据异常类型硬编码网络逻辑，异常映射应在 `NetworkRepo` 或 Parser 侧完成。

## 导航架构

基于 Navigation Compose typed routes，路由使用 `@Serializable` data class 定义：

```kotlin
@Serializable data class SearchRoute(val query: String?, val advancedSearchJson: String?)
@Serializable data class VideoRoute(val videoCode: String, val localUri: String?)
```

关键约定：
- 跳转使用 `navController.navigateSafely(...)` 避免快速点击重复入栈
- 页面间只传路由必要参数，不传大对象
- 复杂搜索参数通过 JSON 字符串承载，进入页面后灌入 ViewModel
- 本地文件播放使用 `VideoRoute(videoCode = "-1", localUri = uri)`

关键文件：`ui/navigation/main/MainRoutes.kt`, `MainNavHost.kt`, `MainNavigationActions.kt`, `NavControllerExt.kt`

## 开发约定

### 代码风格
- UI 优先使用 Jetpack Compose，少量历史页面保留 XML / ViewBinding / DataBinding
- 页面状态通过 `StateFlow` 暴露，一次性事件通过 `SharedFlow` 或回调传递
- 网络和数据库访问不要写进 Composable
- 新 UI 优先使用 Compose，新设置项集中放在 `Preferences` 或明确配置类
- 优先做最小正确修改
- Kotlin code style: `official`（见 `gradle.properties`）

### 列表去重（关键！）
Compose `Lazy*` 使用 `key = { ... }` 时数据源重复 key 会抛 `IllegalArgumentException`。分页合并必须在 ViewModel 层处理去重：
- 视频列表按 `HanimeInfo.videoCode` 去重：`(previous + incoming).distinctBy(HanimeInfo::videoCode)`
- 播放列表按 `Playlists.Playlist.listCode` 去重
- 评论列表使用 `stableKey`

### 异常处理
- `NetworkRepo` 应保留 `CancellationException` 语义，不吞掉协程取消
- 登录态过期、Cloudflare、IP blocked 应映射成可被 UI 理解的异常或状态
- Parser 应容错，DOM 改动时返回明确错误而非静默失败

### 行尾与格式
- 文本文件工作区行尾使用 CRLF
- 文件末尾保留一个换行符

### 排查问题优先看
1. 页面是否重复触发网络请求
2. ViewModel 是否在 Loading 状态下清空了不该清空的列表
3. `Lazy*` key 是否唯一
4. Parser 是否因 DOM 改动解析为空
5. 登录态或 Cookie 是否过期
6. Cloudflare/IP blocked 是否被正确映射

## 常见改动入口

| 改动类型 | 关键文件 |
|---------|---------|
| 新增首页模块 | `HomePageScreen.kt`, `MainViewModel.kt`, `Parser.homePageVer2`, `HomePage` model |
| 新增搜索过滤 | `SearchScreen.kt`, `AdvancedSearchSheet.kt`, `SearchViewModel.kt`, `HAdvancedSearch.kt`, `NetworkRepo.getHanimeSearchResult` |
| 新增视频详情字段 | `HanimeVideo.kt`, `Parser.hanimeVideoVer2`, `VideoIntroductionScreen.kt`, `VideoRouteActions.kt` |
| 新增我的页面列表 | `MainRoutes.kt`, `MainNavHost.kt`, RouteScreen, ViewModel, `NetworkRepo` + `Parser` |
| 新增下载能力 | `DownloadViewModel.kt`, `HanimeDownloadManagerV2.kt`, `HanimeDownloadWorker.kt`, DownloadDatabase/DAO/Entity |
| 新增设置项 | `Preferences.kt`, `HomeSettingsScreen.kt` / 子设置页面, 实际业务读取点 |
| DOM 改版适配 | `Parser.kt`（优先检查） |
| 网络请求异常 | 拦截器, `HCookieJar`, Cloudflare 处理, 备用域名 |

## 依赖管理

依赖版本集中在 `gradle/libs.versions.toml`，使用 version catalog 方式管理。bundles 用于组合常用依赖组 (`android-base`, `android-jetpack`)。

主要依赖：
- Compose BOM: `2026.05.01`
- Firebase BOM: `34.13.0`
- Retrofit: `3.0.0`
- OkHttp: `5.3.2`
- Room: `2.8.4`
- Navigation: `2.9.8`
- Coil Compose: `3.4.0`
- Jsoup: `1.22.2`
- Media3 ExoPlayer: `1.10.1`
- kotlinx-serialization: `1.11.0`

## 公告系统

应用侧：`MainViewModel.loadAnnouncements()` → Firebase Realtime Database `announcements` 节点 → 首页展示 → `AnnouncementDialog` 详情弹窗。用户关闭后 24h 内不再自动弹出。

管理侧：`HanimeAnnouncementManagerWebUI/HanimeAnnouncementManager.html` 基于 Firebase Web SDK 增删改查公告。`PermitAdmin.py` 通过 Firebase Admin SDK 写入 `isAdmin` 自定义声明。

## 隐私与安全

⚠️ 此项目涉及敏感内容，**不要在任何公开平台宣传本软件**。

- 应用支持应用锁、启动器图标伪装、手动 Cookie 管理
- Cloudflare 处理和 IP blocked 需要正确映射到 UI 可理解的异常
- Release 签名相关的环境变量不要暴露或提交
- `ha1_github_token.txt` 已加入 `.gitignore`，不可提交
- `google-services.json` 已加入 `.gitignore`