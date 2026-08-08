# Blaze Browser

A lightweight, fast Android browser built with WebView.

## Features
- Full web browsing with WebView
- URL bar with search integration
- Navigation controls (back, forward, refresh, home)
- Progress indicator
- Dark theme with Blaze orange accent
- JavaScript enabled
- Zoom controls

## Building

### Prerequisites
- JDK 17+
- Android SDK (API 34)
- Gradle 8.5+

### Local Build
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release-unsigned.apk
```

### GitHub Actions
Push to `main` branch or trigger manually to build APKs automatically.

## Project Structure
```
BlazeBrowser/
├── app/
│   ├── src/main/
│   │   ├── java/com/blazebrowser/
│   │   │   └── MainActivity.kt
│   │   ├── res/
│   │   │   ├── layout/activity_main.xml
│   │   │   ├── values/
│   │   │   │   ├── strings.xml
│   │   │   │   ├── colors.xml
│   │   │   │   └── themes.xml
│   │   │   └── drawable/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── .github/workflows/android.yml
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
└── gradle/wrapper/gradle-wrapper.properties
```

## License
MIT