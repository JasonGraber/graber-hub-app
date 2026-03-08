# 📱 Graber Hub — Companion App

Android companion app for [Graber Hub](https://github.com/JasonGraber/graber-hub), a family command center running on a Raspberry Pi.

Control your family's schedule, chores, countdowns, and display mode — all from your phone.

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Android](https://img.shields.io/badge/platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/kotlin-Jetpack%20Compose-purple.svg)

---

## ✨ Features

- **📅 Schedule** — View, add, edit, and delete daily schedule blocks with emoji and time pickers. Includes school day, weekend, and summer templates.
- **✅ Chores** — Manage per-kid chores with tap-to-complete. Assign chores to any kid with emoji categories.
- **⏳ Countdowns** — Track upcoming events with animated countdown cards.
- **⏱️ Timer** — Start countdown timers on the Hub display with alarm sound.
- **📺 Display Mode** — Switch the Hub between Dashboard, Photos, and Auto modes. Persistent mode indicator shows current state on every screen.
- **🖼️ Photo Album** — Configure Google Photos album for the Hub's photo slideshow.
- **📖 Bible Verses** — Add and manage daily Bible verses shown on the Hub.
- **🎨 Kid Colors** — Customize each kid's accent color.
- **📡 Network** — Auto-connects via Home WiFi or Tailscale with one-tap switching.

---

## 🏗️ Tech Stack

- **Kotlin** + **Jetpack Compose** + **Material 3**
- **MVVM** architecture with StateFlow
- **Retrofit** for REST API communication
- **Coroutines** for async operations
- Min SDK 26 / Target SDK 34

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34

### Build
```bash
git clone https://github.com/JasonGraber/graber-hub-app.git
cd graber-hub-app
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

### Configure
On first launch, the app connects to the Hub at `192.168.86.43` (default). Use Settings to:
- Switch between Home WiFi and Tailscale networks
- Enter a custom server URL
- Test connection

---

## 📁 Project Structure

```
app/src/main/java/com/graberhub/companion/
├── MainActivity.kt              # App entry, navigation, display mode indicator
├── alarm/AlarmService.kt         # Timer alarm foreground service
├── data/
│   ├── models/Models.kt         # Data classes (Kid, ScheduleBlock, Chore, etc.)
│   └── prefs/PrefsManager.kt    # SharedPreferences for server URL & kid colors
├── network/
│   ├── ApiService.kt            # Retrofit API interface
│   └── RetrofitClient.kt        # Retrofit singleton with auto-init
├── ui/
│   ├── components/              # Reusable composables
│   │   ├── CommonComponents.kt  # Time pickers, emoji picker
│   │   ├── ColorPickerDialog.kt # Kid color customization
│   │   ├── DisplayModeIndicator.kt # Global display mode chip
│   │   └── EmojiPicker.kt      # Emoji selection grid
│   ├── navigation/NavGraph.kt   # Navigation routes
│   ├── screens/                 # Feature screens
│   │   ├── ScheduleScreen.kt
│   │   ├── ChoresScreen.kt
│   │   ├── CountdownsScreen.kt
│   │   ├── TimerScreen.kt
│   │   └── SettingsScreen.kt
│   └── theme/                   # Material 3 theme (colors, typography)
└── viewmodel/MainViewModel.kt   # Shared ViewModel for all screens
```

---

## 🔗 Related Repos

- **[graber-hub](https://github.com/JasonGraber/graber-hub)** — Web dashboard & API (runs on Raspberry Pi)
- **graber-hub-watch** — Wear OS companion for Pixel Watch *(coming soon)*

---

## 📄 License

MIT License — see [LICENSE](LICENSE) for details.
