# Secure WebView Demo

Android додаток для безпечного перегляду веб-контенту з підтримкою deep links, аналітики та керування доступністю контенту.

## 🛠 Інструкція зі збірки

### Вимоги
- Android Studio Hedgehog | 2023.1.1+
- JDK 17
- Android SDK 34
- Kotlin 1.9.0+

### Кроки збірки

1. **Клонування репозиторію**
```bash
git clone https://github.com/your-username/secure-webview-demo.git
cd secure-webview-demo
```

2. **Синхронізація Gradle**
```bash
./gradlew clean build
```

3. **Запуск**
```bash
./gradlew installDebug
```

### Тестування deep links через ADB
```bash
adb shell am start -W -a android.intent.action.VIEW -d "myapp://game?url=https://news.ycombinator.com&title=Hacker%20News" com.test.app
```

## ✨ Реалізовані функції

### Обов'язкові функції

#### 1. ✅ WebView з безпечним завантаженням
- Блокування небезпечних схем (тільки http/https)
- Редірект HTTP та сторонніх доменів на зовнішній браузер
- Toggle-перемикач доступності контенту
- Валідація URL перед завантаженням

#### 2. ✅ Deep Links
- Схема: `myapp://game?url=<url>&title=<title>`
- Візуальні індикатори (Chip, Card)
- Кастомний title в toolbar
- Кнопка симуляції для тестування

#### 3. ✅ Аналітика подій
- Room Database для зберігання логів
- Real-time відображення подій
- Дублювання подій в Logcat
- Події: завантаження, навігація, помилки, deep links
- Кнопка "Clear Logs"

#### 4. ✅ Обфускація sensitive даних
- Base64 кодування
- ProGuard R8 обфускація
- Централізований `SecureUrlProvider`

### Додаткові функції

- ✅ Адаптивний layout (горизонтальна орієнтація)
- ✅ Scrollable Deep Link Mode
- ✅ Custom Link Bar з доменом та URL
- ✅ No Internet Screen з retry механізмом
- ✅ Dynamic Toolbar Menu (Refresh, Open in Browser)
- ✅ Fragment Navigation з правильним lifecycle
- ✅ Loading states та error handling

## 🏗 Архітектура проекту

### Структура
```
app/
├── activity/           # HomeActivity
├── adapters/           # LogAdapter
├── base/               # BaseActivity, BaseFragment, Application
├── data/               # Room: LogDatabase, AnalyticsLogDao, AnalyticsLogEntity
├── di/                 # DatabaseModule
├── fragment/           # MenuFragment, WebViewContentFragment, NoInternetFragment, ContentUnavailableFragment
├── managers/           # ContentVisibilityManager
├── repositories/       # AnalyticsTracker
├── states/             # MenuUiState, WebViewContentUiState
├── utils/              # SecureUrlProvider, Constants
└── viewModels/         # MenuViewModel, WebViewContentViewModel
```

### Патерн: MVVM + Clean Architecture
```
Activity → Fragments → ViewModels → Repositories → Data Sources (Room, SharedPreferences)
```

### DI: Hilt
```kotlin
@HiltAndroidApp
class App : Application()

@AndroidEntryPoint
class WebViewContentFragment : BaseFragment<...>()

@HiltViewModel
class WebViewContentViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) : ViewModel()
```

## 🔐 Ключові технічні рішення

### 1. Обфускація URL - Багаторівнева система

**Рівень 1: Base64**
```kotlin
private const val ENCODED_URL = "aHR0cHM6Ly9leGFtcGxlLmNvbQ=="
```

**Рівень 4: ProGuard/R8**
```proguard
-keep class com.test.app.utils.SecureUrlProvider { ... }
-obfuscate
```

**Чому така архітектура?**
- AES: криптографічно стійке
- ProGuard: обфускація класів і методів

### 2. WebView Security
```kotlin
// Блокування небезпечних схем
if (uri.scheme != "http" && uri.scheme != "https") return true

// Редірект HTTP та різних доменів
if (url.startsWith("http://") || shouldRedirectToExternalBrowser(url)) {
    openInCustomTabs(url)
    return true
}
```

### 3. Deep Link Handling
```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="myapp" android:host="game" />
</intent-filter>
```
```kotlin
override fun onNewIntent(intent: Intent) {
    val data = intent?.data
    if (data?.scheme == "myapp" && data.host == "game") {
        val url = data.getQueryParameter("url")
        val title = data.getQueryParameter("title")
        navigateToWebView(url, title, isDeepLink = true)
    }
}
```

### 4. Scrollable Deep Link Mode

**Проблема**: Потрібно показати додатковий контент (chip, card) у deep link режимі

**Рішення**: Два окремі layouts
```kotlin
if (isDeepLink) {
    binding.scrollViewDeepLink.visibility = View.VISIBLE  // NestedScrollView
    binding.normalModeContainer.visibility = View.GONE
} else {
    binding.scrollViewDeepLink.visibility = View.GONE
    binding.normalModeContainer.visibility = View.VISIBLE  // Full-screen WebView
}
```

### 5. No Internet Handling
```kotlin
private fun checkInternetConnectivity(): Boolean {
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

// Показуємо NoInternetFragment через view.post {} щоб уникнути FragmentTransaction конфліктів
if (!checkInternetConnectivity()) {
    view?.post { showNoInternetFragment() }
}
```

### 6. Analytics з Room
```kotlin
@Entity(tableName = "analytics_logs")
data class AnalyticsEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val logMessage: String,
    val timestamp: Long
)

@Dao
interface AnalyticsDao {
    @Query("SELECT * FROM analytics_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AnalyticsEntity>>
}
```

## 🚀 Майбутні покращення

### Якби було більше часу:

**Security:**
- Root/Emulator detection
- Biometric authentication

**WebView Features:**
- File download/upload
- Cookie management
- Content blocking (ads/trackers)

**UI/UX:**
- Animations
- Reader mode
- Bookmarks & History

**Architecture:**
- Multi-module
- Compose UI migration
- Use Cases layer

---
