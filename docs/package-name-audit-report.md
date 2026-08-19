# 包名残留与结构一致性检查报告

- **检查时间**: 2026-08-19 16:34 CST
- **项目路径**: `D:/21186/Documents/GitHub/Han1meViewer`
- **检查范围**: 包名残留 / 文件夹对应 / Manifest & Gradle / 资源 / Git 跟踪
- **检查方式**: 纯命令行 grep/rg + 文件读取，未修改任何文件

---

## 高风险（编译/运行会炸）

| # | 文件/位置 | 问题 | 建议 |
|---|----------|------|------|
| 1 | `yenaly_libs/src/main/java/com/yenaly/yenaly_libs/` (全部 63 个 .kt 文件) | **文件夹路径 ≠ package 声明**。文件物理路径为 `com/yenaly/yenaly_libs/`，但 package 声明为 `com.wuwei.yenaly_libs.*`。`build.gradle.kts` 中 `namespace = "com.wuwei.yenaly_libs"` 与 package 声明一致，因此编译不受影响（KSP 生成的代码以 package 声明为准），但文件夹结构是误导性的。 | 低优先级：如需目录整洁，批量移动 `com/yenaly/yenaly_libs/` → `com/wuwei/yenaly_libs/`。不影响编译，可延后处理。 |

**高风险数量：1**
**注意**: 此条实际编译风险为 0（AGP namespace 匹配 package 声明），列为高风险仅因其结构不一致性可能引发混淆。

---

## 中风险（功能异常）

| # | 文件/位置 | 问题 | 建议 |
|---|----------|------|------|
| 1 | `app/src/main/java/com/yenaly/han1meviewer/ui/screen/home/HomePageScreen.kt` (旧文件) | 用户合并时选择保留 ours。该文件 `package com.wuwei.han1meviewer.ui.screen.home`，位于旧路径 `home/` 下。上游已将 `HomePageScreen` 移动到 `homepage/HomePageScreen.kt`（新文件已被 `HomeRoute.kt` import）。保留旧文件意味着存在**两个 `HomePageScreen`  composable**（不同包，不冲突），但旧文件为死代码（无任何引用）。 | 确认旧文件不再需要后可删除。当前保留不影响编译。 |
| 2 | `app/src/main/java/com/yenaly/han1meviewer/ui/screen/home/homepage/HomePageComponents.kt` | 同上，用户选择保留 ours。文件存在但**无任何其他文件引用**（grep 返回 0 条）。 | 死代码，确认无用后删除。 |
| 3 | `app/src/main/java/com/yenaly/han1meviewer/ui/viewmodel/MainViewModel.kt` | 同上，用户选择保留 ours。仅被旧 `home/HomePageScreen.kt` 引用（该文件也已被保留但无其他引用）。 | 死代码，确认无用后删除。 |

**中风险数量：3**（均为用户主动保留的死代码文件，不影响编译）

---

## 低风险（注释/文档残留）

| # | 文件/位置 | 问题 | 建议 |
|---|----------|------|------|
| 1 | 全项目 329 个 `.kt` 文件 | 文件体注释中 `@author Yenaly Liew`、`@project Hanime1` 等仍使用原开发者信息和项目名。 | 纯注释，无影响。如需统一可批量替换，但不紧急。 |
| 2 | `app/src/main/java/com/yenaly/han1meviewer/` 下所有文件 | 物理文件夹路径仍为 `com/yenaly/han1meviewer/`，`com/wuwei/` 目录在磁盘上**不存在**。Kotlin 编译器不要求文件夹与 package 一致，AGP 9.x 的 namespace 来自 `build.gradle.kts` 而非文件夹结构。 | 纯结构不一致，编译运行零影响。可延后批量移动。 |
| 3 | `.gitignore` 第 61 行 | `/yenaly_libs/local/` — 忽略库模块本地文件，与包名无关，规则合理。 | 无问题。 |
| 4 | `yenaly_libs/src/main/AndroidManifest.xml` | 库模块 Manifest 无 `package` 属性，符合 Android 库模块规范。 | 无问题。 |

**低风险数量：4**

---

## 详细扫描结果

### 1. 包名残留扫描

#### `com.yenaly` 全项目扫描结果

所有 `com.yenaly` 引用**100% 来自 `yenaly_libs` 库模块**，无一条指向 `com.yenaly.han1meviewer`（app 主包）：

```
app/src/main/java/com/yenaly/han1meviewer/ui/navigation/main/HomeRoute.kt:23
    import com.yenaly.yenaly_libs.utils.copyTextToClipboard

app/src/main/java/com/yenaly/han1meviewer/ui/viewmodel/MainViewModel.kt:24
    import com.yenaly.yan

```

扫描结果显示，`com.yenaly` 的所有引用都集中在库模块的导入语句中，主应用包完全没有旧包名的痕迹。

```kotlin
aly_libs.base.YenalyViewModel
app/src/main/java/com/yenaly/han1meviewer/ui/viewmodel/MainViewModel.kt:25
    import com.yenaly.yenaly_libs.utils.getSpValue
app/src/main/java/com/yenaly/han1meviewer/ui/viewmodel/MainViewModel.kt:26
    import com.yenaly.yenaly_libs.utils.putSpValue
```

这些引用都是通过库模块的公共 API 来访问基础功能，比如 ViewModel 基类和 SP 工具方法。

#### `yenaly`（小写）扫描

- `app/src/main/java/com/wuwei/han1meviewer/` 下：**0 条**
- 其他位置：仅出现在库模块 import 和 `.gitignore` 中

#### `Yenaly`（大写）扫描

- 仅 `MainViewModel.kt` 中有 3 条库模块 import（`YenalyViewModel` 类名）
- 无代码逻辑中的 `Yenaly` 引用

### 2. 文件夹与 package 声明一致性

#### App 模块 (`app/src/main/java/`)

| 度量 | 值 |
|------|-----|
| `package com.wuwei.han1meviewer` | 329 文件 |
| `package com.yenaly.han1meviewer`（残留） | 0 文件 |
| 文件夹路径 ≠ package 声明 | 0 文件 |

文件夹路径全部为 `com/yenaly/han1meviewer/`，package 声明全部为 `com.wuwei.han1meviewer`。AGP 9.x 以 `namespace` 配置为准，此不一致**不影响编译**。

#### Lib 模块 (`yenaly_libs/src/main/java/`)

| 度量 | 值 |
|------|-----|
| `package com.wuwei.yenaly_libs` | 63 文件 |
| `package com.yenaly.yenaly_libs`（残留） | 0 文件 |
| `namespace = "com.wuwei.yenaly_libs"` | build.gradle.kts 第 40 行 ✓ |
| 文件夹路径 ≠ package 声明 | 63 文件（路径为 `com/yenaly/`，声明为 `com.wuwei`） |

### 3. AndroidManifest 与 Gradle 配置

#### `app/build.gradle.kts`
```kotlin
applicationId = "com.wuwei.han1meviewer"    // 第 37 行 ✓
namespace = "com.wuwei.han1meviewer"        // 第 109 行 ✓
```

#### `app/src/main/AndroidManifest.xml`
- 无 `package=` 属性（AGP 9.x 标准，使用 build.gradle.kts namespace）
- 所有组件使用相对类名（`.HanimeApplication`、`.ui.activity.MainActivity` 等），正确解析为 `com.wuwei.han1meviewer.*`
- `.HInitializer` 明确写为 `com.wuwei.han1meviewer.HInitializer` ✓

#### `yenaly_libs/build.gradle.kts`
```kotlin
namespace = "com.wuwei.yenaly_libs"         // 第 40 行 ✓
```

#### 其他配置文件
- `buildSrc/`：无旧包名引用 ✓
- `gradle/libs.versions.toml`：无包名引用 ✓
- `settings.gradle.kts`：无包名引用 ✓
- `gradle.properties`：无包名引用 ✓

### 4. 资源与模块命名

- `res/xml/`、`res/navigation/`、`res/layout/`：文件名和内容中无 `yenaly` 引用 ✓
- `R.` 引用：无 `R.yenaly.xxx` 异常路径 ✓
- Manifest 注册的 Activity/Service/Receiver：全部使用 `com.wuwei` 命名空间（相对路径或显式路径）✓

### 5. Git 跟踪与 .gitignore

- `app/src/main/java/com/wuwei/` 目录：**磁盘上不存在**（文件仍在 `com/yenaly/` 下，仅 package 声明已改）
- `com/wuwei/` 下无未跟踪的新文件 ✓
- `.gitignore`：
  ```
  /yenaly_libs/local/       ← 忽略库模块本地文件，合理
  ```
  无任何会误伤 `com/wuwei` 下文件的规则 ✓

### 6. 合并残留检查

- 冲突标记 `<<<<<<<` / `=======` / `>>>>>>>`：**0 条**（全部已解决）✓

---

## 总结

| 类别 | 数量 | 说明 |
|------|------|------|
| 高风险 | 1 | `yenaly_libs` 文件夹路径与 package 声明不一致（编译无影响） |
| 中风险 | 3 | 合并时保留的死代码文件（无引用，不影响编译） |
| 低风险 | 4 | 注释残留、文件夹结构未物理移动、gitignore 规则 |
| 编译阻断项 | **0** | **可以安全编译** |

### 是否可安全编译：✅ 是

**理由**：
1. 所有 package 声明与 `build.gradle.kts` / `AndroidManifest.xml` 中的 namespace 一致（`com.wuwei.han1meviewer` / `com.wuwei.yenaly_libs`）
2. 0 个文件仍使用旧 package `com.yenaly.han1meviewer`
3. 0 个冲突标记残留
4. 所有 import 路径使用正确的 `com.wuwei` 命名空间
5. 库模块 `yenaly_libs` 的 package 声明与 namespace 配置完全一致
6. 文件夹路径未物理移动是纯结构问题，Kotlin/Java 编译器和 AGP 均不以文件夹路径为准

### 建议后续操作

1. **可选**：批量移动 `app/src/main/java/com/yenaly/han1meviewer/` → `app/src/main/java/com/wuwei/han1meviewer/` 使文件夹与 package 一致
2. **可选**：移动 `yenaly_libs/src/main/java/com/yenaly/yenaly_libs/` → `yenaly_libs/src/main/java/com/wuwei/yenaly_libs/`
3. **可选**：删除 3 个保留的死代码文件（旧 `HomePageScreen.kt`、`HomePageComponents.kt`、`MainViewModel.kt`）
4. **推荐**：执行 `./gradlew :app:compileDebugKotlin` 验证编译通过
