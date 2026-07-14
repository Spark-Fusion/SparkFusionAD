# SparkFusionAd SDK

[![](https://jitpack.io/v/Spark-Fusion/SparkFusionAD.svg)](https://jitpack.io/#Spark-Fusion/SparkFusionAD)

一个轻量级的 Android 广告测试 SDK，支持多种广告类型，帮助开发者快速集成广告功能。

## 功能特性

- ✅ **开屏广告** - 应用启动时展示的全屏广告
- ✅ **Banner 广告** - 页面底部的横幅广告
- ✅ **插屏广告** - 页面切换时展示的插屏广告（支持概率控制）
- ✅ **激励视频广告** - 用户观看完整视频后可获得奖励的广告

## 环境要求

- Android Studio
- JDK 11+
- Android SDK 29+ (Android 10.0)
- Kotlin 1.9.0+

## 快速开始

### 1. 添加依赖

#### Step 1: 添加 JitPack 仓库

在项目的 `settings.gradle.kts` 中添加 JitPack 仓库：

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

如果使用 `settings.gradle`（Groovy），添加：

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

#### Step 2: 添加依赖

在模块的 `build.gradle.kts` 中添加：

```kotlin
dependencies {
    implementation("com.github.Spark-Fusion:SparkFusionAD:1.0.0")
}
```

如果使用 `build.gradle`（Groovy），添加：

```groovy
dependencies {
    implementation 'com.github.Spark-Fusion:SparkFusionAD:1.0.0'
}
```

**JitPack 地址**: [https://jitpack.io/#Spark-Fusion/SparkFusionAD/1.0.0](https://jitpack.io/#Spark-Fusion/SparkFusionAD/1.0.0)

### 2. 初始化 SDK

#### 方式一：直接在 Application 中初始化（推荐）

在 `Application` 类中初始化：

```kotlin
import com.sparkfusionad.sdk.SparkFusionAd

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SparkFusionAd.initSparkFusionAd(
            this,
            "你的 AppKey"
        )
    }
}
```

在 `AndroidManifest.xml` 中注册 Application：

```xml
<application
    android:name=".MyApplication"
    ...>
    ...
</application>
```

#### 方式二：使用 AppContextHolder（参考项目实现）

如果您的项目需要全局 Application 管理，可以参考以下实现：

**1. 创建 AppContextHolder：**

```kotlin
@SuppressLint("StaticFieldLeak")
object AppContextHolder {
    private lateinit var application: Application

    fun init(app: Application) {
        this.application = app
    }

    fun getApplication(): Application {
        if (!::application.isInitialized) {
            throw IllegalStateException("AppContextHolder is not initialized")
        }
        return application
    }
}
```

**2. 在 Application 中初始化：**

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContextHolder.init(this)
    }
}
```

**3. 创建 SdkManager 管理 SDK 初始化：**

```kotlin
object SdkManager {
    /**
     * 初始化广告
     */
    fun initAd(): Boolean {
        return try {
            SparkFusionAd.initSparkFusionAd(
                AppContextHolder.getApplication(),
                "你的 AppKey"
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}
```

**4. 在需要的地方调用初始化：**

```kotlin
if (SdkManager.initAd()) {
    // 初始化成功，可以使用广告功能
    SparkFusionAd.loadSFSplashAd(context, "开屏广告位id")
} else {
    // 初始化失败，处理错误
}
```

### 3. 使用示例

#### 开屏广告

```kotlin
// 加载开屏广告
SparkFusionAd.loadSFSplashAd(
    context = context,
    adId = "开屏广告位id",
    loadThirdPartyAd = {
        // 加载第三方广告
    },
    listener = SparkFusionAdLoadListener(
        onAdLoadSuccess = {
            val splashContainer = findViewById<ViewGroup>(R.id.splash_container)
            SparkFusionAd.showSFSplashAd(
                splashContainer,
                SparkFusionAdShowListener(
                    onAdShowSuccess = {},
                    onAdShowFailure = {},
                    onAdClick = {},
                    onAdClose = {
                        // 跳转到主界面
                    }
                )
            )
        },
        onAdLoadFailure = {
            // 广告加载失败
        }
    )
)
```

#### Banner 广告

```kotlin
val bannerContainer = findViewById<ViewGroup>(R.id.banner_container)

SparkFusionAd.loadSFBannerAd(
    context = this,
    adId = "Banner广告位id",
    listener = SparkFusionAdLoadListener(
        onAdLoadSuccess = {
            SparkFusionAd.showSFBannerAd(
                bannerContainer,
                SparkFusionAdShowListener(
                    onAdShowSuccess = {},
                    onAdShowFailure = {},
                    onAdClick = {},
                    onAdClose = {}
                )
            )
        },
        onAdLoadFailure = {}
    )
)

// 移除 Banner 广告
SparkFusionAd.removeSFBannerAd(bannerContainer)
```

#### 插屏广告

```kotlin
SparkFusionAd.loadSFInterstitialAd(
    context = this,
    adId = "插屏广告位id",
    listener = SparkFusionAdLoadListener(
        onAdLoadSuccess = {
            SparkFusionAd.showSFInterstitialAd(
                this,
                SparkFusionAdShowListener(
                    onAdShowSuccess = {},
                    onAdShowFailure = {},
                    onAdClick = {},
                    onAdClose = {}
                )
            )
        },
        onAdLoadFailure = {}
    )
)
```

#### 激励视频广告

```kotlin
SparkFusionAd.loadSFVideoAd(
    context = this,
    adId = "激励视频广告位id",
    listener = SparkFusionAdLoadListener(
        onAdLoadSuccess = {
            SparkFusionAd.showSFVideoAd(
                activity = this,
                listener = SparkFusionRewardAdShowListener(
                    onAdShowSuccess = {},
                    onAdShowFailure = {},
                    onAdClick = {},
                    onAdClose = {},
                    onReward = {
                        // 发放奖励
                    }
                )
            )
        },
        onAdLoadFailure = {}
    )
)
```

## API 文档

### 初始化

#### `initSparkFusionAd(context: Context, appKey: String)`

初始化 SparkFusionAd SDK。

**参数：**
- `context`: 应用上下文
- `appKey`: FreelyBase AppKey

---

### 开屏广告

#### `loadSFSplashAd(context: Context, adId: String, thirdPartyLoader: SparkFusionThirdPartyAdLoader? = null, listener: SparkFusionAdLoadListener)`

加载开屏广告。

**参数：**
- `context`: 上下文
- `adId`: 开屏广告位 id
- `listener`: 加载成功/失败监听

#### `showSFSplashAd(view: ViewGroup, listener: SparkFusionAdShowListener)`

显示开屏广告。

**参数：**
- `view`: 用于显示广告的容器视图
- `listener`: 展示成功、展示失败、点击、关闭监听

---

### Banner 广告

#### `loadSFBannerAd(context: Context, adId: String, listener: SparkFusionAdLoadListener)`

加载 Banner 广告。

**参数：**
- `context`: 上下文
- `adId`: Banner 广告位 id
- `listener`: 加载成功/失败监听

#### `showSFBannerAd(view: ViewGroup, listener: SparkFusionAdShowListener)`

显示 Banner 广告。

**参数：**
- `view`: 用于显示广告的容器视图
- `listener`: 展示成功、展示失败、点击、关闭监听

#### `removeSFBannerAd(view: ViewGroup)`

移除 Banner 广告。

**参数：**
- `view`: 广告容器视图

---

### 插屏广告

#### `loadSFInterstitialAd(context: Context, adId: String, listener: SparkFusionAdLoadListener)`

加载插屏广告。

**参数：**
- `context`: 上下文
- `adId`: 插屏广告位 id
- `listener`: 加载成功/失败监听

#### `showSFInterstitialAd(activity: Activity, listener: SparkFusionAdShowListener)`

显示插屏广告。

**参数：**
- `activity`: Activity 上下文
- `listener`: 展示成功、展示失败、点击、关闭监听

---

### 激励视频广告

#### `loadSFVideoAd(context: Context, adId: String, listener: SparkFusionAdLoadListener)`

加载激励视频广告。

**参数：**
- `adId`: 激励视频广告位 id
- `listener`: 加载成功/失败监听

#### `showSFVideoAd(activity: Activity, listener: SparkFusionRewardAdShowListener)`

显示激励视频广告。

**参数：**
- `activity`: Activity 上下文
- `listener`: 展示成功、展示失败、点击、关闭、奖励监听

---

### 资源管理

#### `destroy()`

销毁 SDK 资源，释放内存。建议在应用退出时调用。

---

## 项目结构

```
SparkFusionADSDK/
├── app/                    # 示例应用
│   └── src/main/
│       └── java/com/sparkfusionad/app/
├── library/                # SDK 库模块
│   └── src/main/
│       └── java/com/sparkfusionad/sdk/
│           └── SparkFusionAd.kt
├── build.gradle.kts        # 项目构建配置
└── settings.gradle.kts     # 项目设置
```

## 版本信息

- **当前版本**: 1.0.0
- **最低 SDK 版本**: 29 (Android 10.0)
- **编译 SDK 版本**: 35
- **Kotlin 版本**: 1.9.0

## 依赖库

- `androidx.core:core-ktx:1.10.1`
- `androidx.appcompat:appcompat:1.6.1`
- `com.google.android.material:material:1.10.0`

## 注意事项

1. **初始化顺序**：使用任何广告功能前，必须先调用 `initSparkFusionAd()` 进行初始化
2. **生命周期管理**：建议在 `Application` 的 `onCreate()` 中初始化 SDK
3. **资源释放**：应用退出时建议调用 `destroy()` 释放资源
4. **线程安全**：所有 API 调用应在主线程进行
5. **插屏广告概率**：`probability` 参数控制广告显示频率，值越大显示概率越小

## 许可证

本项目采用 MIT 许可证。详情请参阅 LICENSE 文件。

## 贡献

欢迎提交 Issue 和 Pull Request！

## 联系方式

如有问题或建议，请通过以下方式联系：

- 提交 Issue
- 发送邮件

---

**SparkFusionAd SDK** - 让广告集成更简单 🚀
