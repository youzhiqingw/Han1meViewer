# Han1meViewer 安卓编译指南

> 基于项目 `misaka10032w/Han1meViewer` 源码及 Firebase 官方文档整理。

---

## 1. 环境准备

| 工具           | 版本要求                               | 说明                          |
| -------------- | -------------------------------------- | ----------------------------- |
| Android Studio | Narwhal 2025.1.1+                      | 包含 SDK Manager、AVD Manager |
| JDK            | 21                                     | AGP 9.2.x 要求                |
| Android SDK    | compileSdk 36, minSdk 27, targetSdk 37 | 通过 SDK Manager 安装         |
| Gradle         | 由 Wrapper 自动管理 (8.10+)            | 无需手动安装                  |
| Kotlin         | 2.3.x                                  | 由 Wrapper 自动管理           |

### 在 Android Studio 中安装 SDK

1. 打开 Android Studio → **Settings → Languages & Frameworks → Android SDK**
2. 勾选以下组件进行安装：
   - Android SDK Platform 36
   - Android SDK Build-Tools 36.x
   - Google Play services
   - Google Repository
3. 在 **SDK Platforms** 标签页确认目标 API Level 已安装

---

## 2. 克隆仓库

```bash
git clone https://github.com/misaka10032w/Han1meViewer.git
cd Han1meViewer
```

---

## 3. Firebase 配置

### 3.1 创建 Firebase 项目

1. 访问 [Firebase 控制台](https://console.firebase.google.com/)
2. 点击 **添加项目** → 输入项目名称 → 点击 **继续**
3. （可选）启用 Google Analytics
4. 点击 **创建项目**

### 3.2 注册 Android 应用

1. 在 Firebase 项目概览页点击 Android 图标
2. 输入 Android 软件包名：`com.wuwei.han1meviewer`
   - 打包名区分大小写，不可修改
3. （可选）填写应用昵称
4. 点击 **注册应用**

### 3.3 下载配置文件

1. 点击 **下载 google-services.json**
2. 将文件移动到项目 `app/` 目录下（即 `app/google-services.json`）

> 注意：此文件已被 `.gitignore` 排除，不会意外提交到版本库。

### 3.4 Firebase 产品依赖

项目使用以下 Firebase 产品（已内置在 `app/build.gradle.kts` 中）：

| 产品                       | 用途                   |
| -------------------------- | ---------------------- |
| Firebase Analytics         | 统计与分析             |
| Firebase Crashlytics       | 崩溃报告               |
| Firebase Remote Config     | 远程配置（公告等）     |
| Firebase Realtime Database | 实时数据库（公告存储） |
| Firebase Performance       | 性能监控               |

---

## 4. GitHub API Token 配置

更新检查功能需要 GitHub Personal Access Token。

### 4.1 创建 Token

1. 访问 https://github.com/settings/tokens/new
2. **Note**：填写 `Han1meViewer Update Check`
3. **Expiration**：选择 `No expiration` 或较长有效期
4. **Scopes**：勾选 `public_repo`（公开仓库只读，足够用）
5. 点击 **Generate token** → **立即复制**

### 4.2 使用 Token（二选一）

**方式 A — 本地文件（推荐日常开发）：**

在项目根目录创建文件 `app/ha1_github_token.txt`，内容直接粘贴 token：

```
your_github_pat_token_here
```

> 此文件已被 `.gitignore` 排除。

**方式 B — 环境变量（CI / 命令行）：**

```powershell
# PowerShell
$env:HA_GITHUB_TOKEN = "your_github_pat_token_here"
```

```bash
# Git Bash / Linux / macOS
export HA_GITHUB_TOKEN="your_github_pat_token_here"
```

> 构建时 Gradle 读取优先级：GitHub Secrets(CI) > 环境变量 > 本地文件

---

## 5. Release 签名配置（可选，仅 Release 构建需要）

### 5.1 生成签名密钥

```bash
# 生成 JKS 密钥库（仅首次需要）
keytool -genkeypair -v -keystore ~/.android/keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias han1me
```

### 5.2 设置环境变量

```powershell
# PowerShell
$env:KEYSTORE_PASSWORD = "你的密钥库密码"
$env:KEY_ALIAS = "han1me"
```

> Debug 构建不需要签名配置。

---

## 6. 构建选项

### 6.1 Debug 构建

```powershell
# PowerShell
.\gradlew.bat :app:assembleDebug
```

输出 APK：

- 路径：`app/build/outputs/apk/debug/com.wuwei.han1meviewer.debug-<version>.apk`
- 应用 ID：`com.wuwei.han1meviewer.debug`
- 签名：未签名
- 混淆：关闭
- ABI：通用（包含所有架构）

### 6.2 Lint 检查

```powershell
.\gradlew.bat :app:lintDebug
```

### 6.3 单元测试

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

### 6.4 运行到设备/模拟器

1. 启动模拟器或连接真机
2. 点击 Android Studio 的 Run 按钮，或执行：

```powershell
.\gradlew.bat :app:installDebug
```

### 6.5 Release 构建

```powershell
.\gradlew.bat :app:assembleRelease
```

输出 APK：

- 路径：`app/build/outputs/apk/release/Han1meViewer-v<version>.apk`
- 应用 ID：`com.wuwei.han1meviewer`
- 签名：已签名（需配置 `KEYSTORE_PASSWORD` + `KEY_ALIAS`）
- 混淆：开启 ProGuard/R8
- ABI：仅 `arm64-v8a`

### 6.6 Clean 构建（修改 Room 实体后）

```powershell
.\gradlew.bat :app:clean build
```

---

## 7. 模拟器（AVD）配置

1. Android Studio → **Tools → AVD Manager**
2. 点击 **Create Virtual Device**
3. 选择一个设备（推荐 Pixel 7 或类似）
4. 选择系统镜像（建议 API Level 36，Google Play 版本以支持 Firebase）
5. 完成创建后点击 Run

> 注意：Firebase SDK 依赖的设备需要安装 Google Play 服务。

---

## 8. 项目结构速览

```
Han1meViewer/
├─ app/                          # 主应用模块
│  ├─ src/main/java/com/yenaly/han1meviewer/
│  │  ├─ HanimeApplication.kt    # Application 入口
│  │  ├─ Constants.kt            # 全局常量
│  │  ├─ Preferences.kt          # SharedPreferences + StateFlow
│  │  ├─ logic/
│  │  │  ├─ NetworkRepo.kt       # 网络请求统一入口
│  │  │  ├─ DatabaseRepo.kt      # 数据库统一入口
│  │  │  ├─ Parser.kt            # HTML/DOM 解析器
│  │  │  ├─ model/               # 数据模型
│  │  │  ├─ network/             # Retrofit 服务、OkHttp 拦截器
│  │  │  ├─ dao/                 # Room DAO
│  │  │  ├─ entity/              # Room 实体
│  │  │  ├─ exception/           # 自定义异常
│  │  │  └─ state/               # UI 状态封装
│  │  ├─ worker/                 # WorkManager 任务
│  │  ├─ ui/
│  │  │  ├─ screen/              # 功能页面 Compose
│  │  │  ├─ navigation/          # 导航路由
│  │  │  ├─ viewmodel/           # ViewModel
│  │  │  └─ StateLayoutMixin.kt  # 状态布局混合
│  │  └─ util/                   # 工具类
│  ├─ build.gradle.kts           # 模块级构建配置
│  └─ src/main/assets/h_keyframes/ # 关键帧数据
├─ yenaly_libs/                  # 共享基础库
├─ buildSrc/src/main/java/Config.kt  # 版本管理
├─ build.gradle.kts              # 项目级构建配置
├─ settings.gradle.kts           # 模块配置
└─ gradle/libs.versions.toml     # 依赖版本目录
```

---

## 9. 数据流架构

```
Compose Screen
    → ViewModel
    → NetworkRepo / DatabaseRepo
    → Retrofit + Jsoup / Room (KSP)
    → StateFlow
    → Compose Screen
```

### 核心数据流示例（视频页面）

```
VideoRoute → VideoViewModel → NetworkRepo.getHanimeVideo
  → Parser::hanimeVideoVer2 → HanimeVideo
  → VideoScreen / Player / CommentScreen
```

---

## 10. 构建变体差异

| 属性                | Debug              | Release              |
| ------------------- | ------------------ | -------------------- |
| applicationIdSuffix | `.debug`         | 无                   |
| 混淆                | 关闭               | 开启 (R8 + ProGuard) |
| 代码压缩            | 关闭               | 开启                 |
| ABI                 | 通用 (universal)   | 仅 arm64-v8a         |
| 签名                | 无                 | 需 keystore          |
| 版本号策略          | 固定 (commmit SHA) | UTC 时间戳           |
| `COMMIT_SHA`      | 完整 SHA           | 完整 SHA             |

---

## 11. CI/CD 构建

项目的 GitHub Actions 在工作流中自动完成以下操作：

1. 从 GitHub Secrets 解码 `google-services.json`（BASE64）→ 写入 `app/`
2. 从 GitHub Secrets 解码 keystore → 写入 `~/.android/keystore.jks`
3. 设置 `HA_GITHUB_TOKEN` 和 `KEYSTORE_PASSWORD` 等环境变量
4. 执行 `assembleRelease` 构建签名 APK
5. 上传 APK 作为 GitHub Actions 产物

CI 产物和 GitHub Releases 是唯一官方分发渠道。

---

## 12. 常见问题

| 问题                            | 原因                                          | 解决                                    |
| ------------------------------- | --------------------------------------------- | --------------------------------------- |
| `google-services.json` 找不到 | 文件未放在 `app/` 目录                      | 按 3.3 下载后放入                       |
| 签名失败                        | `KEYSTORE_PASSWORD` 或 `KEY_ALIAS` 未设置 | 设置环境变量或仅构建 Debug              |
| 更新检查 401/403                | `HA_GITHUB_TOKEN` 未配置                    | 按 4 节配置或使用 CI 构建               |
| 修改 Room 实体后编译报错        | KSP 注解处理器需要重新生成                    | 执行 `.\gradlew.bat :app:clean build` |
| Firebase 初始化失败             | 模拟器无 Google Play 服务                     | 使用带 Google Play 的系统镜像           |
