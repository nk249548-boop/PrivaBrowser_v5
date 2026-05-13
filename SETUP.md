# PrivaBrowser - Setup Guide

## ⚠️ gradle-wrapper.jar Missing - Fix karo

Yeh ek required step hai:

### Android Studio mein (Recommended):
1. Project Android Studio mein open karo
2. Android Studio automatically suggest karega "Gradle Sync" - **Accept karo**
3. Ya Terminal mein run karo:
   ```
   gradle wrapper --gradle-version 8.5
   ```

### Manual Download:
`gradle/wrapper/gradle-wrapper.jar` yahan se download karo:
```
https://github.com/gradle/gradle/raw/v8.5.0/gradle/wrapper/gradle-wrapper.jar
```
Aur `gradle/wrapper/` folder mein rakh do.

### Script se (Linux/Mac):
```bash
chmod +x setup.sh
./setup.sh
```

---

## Project Structure

```
PrivaBrowser/
├── app/src/main/
│   ├── java/com/privabrowser/
│   │   ├── BiometricLockActivity.kt  ← App launch screen
│   │   ├── HomeActivity.kt           ← New tab / home page
│   │   ├── MainActivity.kt           ← Browser screen
│   │   ├── AdBlocker.kt              ← Ad blocking engine
│   │   ├── AppDatabase.kt            ← Room DB (video playlist)
│   │   ├── VideoDownloadService.kt   ← Background downloader
│   │   └── PlaylistActivity.kt       ← Downloaded videos list
│   ├── assets/
│   │   ├── hosts.txt                 ← Ad block hosts list
│   │   └── easylist.txt              ← EasyList rules
│   └── res/
│       ├── layout/                   ← All XML layouts
│       ├── drawable/                 ← Backgrounds, icons
│       └── values/                   ← Colors, themes, strings
├── gradle/wrapper/
│   ├── gradle-wrapper.properties
│   └── gradle-wrapper.jar           ← ⚠️ Manually add karo
└── build.gradle
```

## Build Requirements
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 34
- Gradle 8.5
