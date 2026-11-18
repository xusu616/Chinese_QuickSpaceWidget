# QuickSpace Widget

A standalone, rootless Android home screen widget (Android 8.0–16 / API 26–36) bringing **crDroid QuickSpace** dates, time-of-day greetings, and bilingual (Simplified Chinese & English) inspirational/easter-egg quotes to any Android ROM or launcher, configured with a Material 3 Jetpack Compose app.

## Features

- **crDroid QuickSpace Authentic Content**
  - Exact crDroid date formatting (`EEE, MMM d` / `EEEE, MMMM d` via ICU `DateFormat`) and all 6 quote categories (`morning`, `noon`, `early_evening`, `evening`, `midnight`, `random`) in both Simplified Chinese and English.
  - **Minimal Double-Line** and **Extended Greeting** (`Good morning. Today is ...` / `早上好。今天是 ...`) layout styles.
  - Optional **crDroid Original Probability Hide** switch (~50% chance to hide the subtitle + gap hours, matching upstream `QuickEventsController.java`) and **Random Easter Eggs + crDroid Icon** switch.
- **Dual Render Engines**
  - **Canvas Double Shadow**: Reproduces crDroid's `DoubleShadowTextView` two-pass ambient (`3dp`) + key (`2dp`, `0.5dp` offset) shadow shader into a crisp widget bitmap.
  - **Native TextView**: Uses lightweight standard `RemoteViews` `TextView` rendering with optional shadow.
- **Instant Bilingual Switching**: Switch between **System**, **简体中文**, and **English** inside the Material 3 app and immediately sync both the app UI and home screen widgets.
- **Battery-Friendly Refresh**: 30-minute system `AppWidget` + inexact `AlarmManager` updates, midnight rollover, date/time/timezone/locale change receivers, screen-unlock check while cached, and tap-to-next-quote support—with zero foreground services.

## Build & Signing

- Built with Kotlin, Jetpack Compose Material 3, AGP `9.0.1`, Gradle `9.3.0`, `minSdk = 26`, `targetSdk = 36`.
- Configure release signing in `local.properties` or `keystore.properties` (ignored by Git):
  ```properties
  RELEASE_STORE_FILE=/path/to/your.jks
  RELEASE_STORE_PASSWORD=...
  RELEASE_KEY_ALIAS=...
  RELEASE_KEY_PASSWORD=...
  ```
- Build unit tests and release APK:
  ```bash
  ./gradlew testDebugUnitTest assembleRelease
  ```

## Attribution & License

- Licensed under the **Apache License 2.0** (`LICENSE`).
- QuickSpace logic, vector icons, and multilingual quote/greeting strings are adapted from [`crdroidandroid/android_packages_apps_Launcher3`](https://github.com/crdroidandroid/android_packages_apps_Launcher3) (Apache-2.0).
