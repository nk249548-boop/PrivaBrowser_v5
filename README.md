# 🔐 PrivaBrowser — Android Private Browser

Ek fully-featured private WebView browser jisme ad blocker, video downloader, playlist, aur biometric lock hai.

---

## 📁 Project Structure

```
PrivaBrowser/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/privabrowser/
│   │   ├── BiometricLockActivity.kt   ← Entry point (fingerprint/PIN lock)
│   │   ├── MainActivity.kt            ← Main browser + ad blocker + video detect
│   │   ├── AdBlocker.kt               ← EasyList + hosts file blocking
│   │   ├── AppDatabase.kt             ← Room DB (VideoEntity, VideoDao)
│   │   ├── PlaylistActivity.kt        ← Saved videos playlist
│   │   └── VideoDownloadService.kt    ← Background download service
│   └── res/
│       ├── layout/
│       │   ├── activity_biometric_lock.xml
│       │   ├── activity_main.xml
│       │   ├── activity_playlist.xml
│       │   └── item_video.xml
│       ├── xml/
│       │   ├── network_security_config.xml
│       │   └── file_paths.xml
│       └── values/themes.xml
├── build.gradle
└── settings.gradle
```

---

## ⚙️ Setup Steps

### Step 1: Android Studio mein open karo
```
File → Open → PrivaBrowser folder select karo
```

### Step 2: Ad Block files add karo (assets folder)

`app/src/main/assets/` folder banao aur yeh files daalo:

**hosts.txt** — Download karo:
```
https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts
```

**easylist.txt** — Download karo:
```
https://easylist.to/easylist/easylist.txt
```

> ⚠️ EasyList bahut bada hai (~300k rules). Sirf pehle 50,000 lines use karo performance ke liye:
> ```bash
> head -n 50000 easylist.txt > easylist_trimmed.txt
> ```

### Step 3: Gradle Sync karo
```
Tools → Sync Project with Gradle Files
```

### Step 4: Run karo
```
Run → Run 'app' → Apna device/emulator choose karo
```

---

## 🚀 Features

| Feature | Implementation |
|---------|---------------|
| 🔐 Biometric Lock | `BiometricLockActivity.kt` — Fingerprint + PIN fallback |
| 🚫 Ad Blocker | `AdBlocker.kt` — Hosts file + EasyList filter rules |
| ⬇️ Video Downloader | `MainActivity.kt` — .mp4/.m3u8 detect + DownloadManager |
| 📋 Playlist | `PlaylistActivity.kt` + Room DB |
| 🕶️ Privacy Mode | No cookies, cache clear on exit, HTTPS only |
| 🔒 Network Security | `network_security_config.xml` — HTTP blocked |

---

## 🎮 Usage Guide

### Browser Use karna
1. App open hogi → Biometric unlock screen aayega
2. Fingerprint ya PIN se unlock karo
3. URL bar mein address ya search query type karo
4. Enter ya ▶ button dabao

### Video Download karna
1. Koi video wali website kholo (YouTube nahi, direct .mp4 sites)
2. Jab video detect ho, **"⬇ Video"** button orange color mein dikhega
3. Button dabao → Download dialog aayega
4. Video Downloads folder mein save hogi

### Playlist dekhna
1. Top bar mein playlist icon dabao
2. Saare saved videos dikhenge
3. ▶ play karo ya 🗑 delete karo

### Browsing data clear karna
1. Bottom bar mein 🗑 (red) button dabao
2. Cache, cookies, history sab clear ho jaayega

---

## 🔧 Customization

### Default Homepage change karna
`MainActivity.kt` mein:
```kotlin
const val HOME_URL = "https://duckduckgo.com"  // Apna preferred search engine
```

### Private mode off karna (data persist karne ke liye)
`MainActivity.kt` onStop() mein:
```kotlin
if (prefs.getBoolean("private_mode", false)) {  // true → false
```

### Custom block list add karna
`AdBlocker.kt` mein `hardcodedBlockList` set mein domain add karo:
```kotlin
"yourdomain.com", "ads.example.com"
```

---

## ⚠️ Limitations

- **YouTube videos**: YouTube ka DRM protection hai, direct download nahi hogi
- **Chrome Extensions**: WebView mein real .crx extension support nahi, sirf JS injection se limited functionality
- **M3U8 streams**: HLS streams detect hote hain lekin download ke liye FFmpeg chahiye
- **EasyList**: Puri list (300k+ rules) performance slow kar sakti hai

---

## 📦 Dependencies

```gradle
androidx.biometric:biometric:1.2.0-alpha05   // Fingerprint
androidx.room:room-runtime:2.6.1             // Database
com.squareup.okhttp3:okhttp:4.12.0           // HTTP
kotlinx-coroutines-android:1.7.3             // Async
```

---

## 🔐 Privacy Policy (Built-in)

- ✅ No data sharing with third parties
- ✅ No analytics or tracking
- ✅ Local-only database
- ✅ HTTPS-only (HTTP blocked)
- ✅ Third-party cookies blocked
- ✅ Browsing data auto-cleared on exit
- ❌ No cloud sync
- ❌ No account required
