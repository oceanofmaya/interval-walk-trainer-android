# Building and Testing Guide

## Prerequisites

1. **Java Development Kit (JDK) 21**
   - Download from: <https://adoptium.net/>
   - macOS: `brew install openjdk@21`
   - Windows: Use installer or `choco install openjdk21`

2. **Android Studio** (includes Android SDK)
   - Download from: <https://developer.android.com/studio>

## Quick Start (Android Studio)

1. Open Android Studio → File → Open → Select project folder
2. Wait for Gradle sync
3. Configure JDK 21 (if needed): File → Project Structure → SDK Location → JDK location
   - macOS: `/opt/homebrew/opt/openjdk@21`
   - Windows: `C:\Program Files\Eclipse Adoptium\jdk-21.x.x-hotspot`
   - Linux: `/usr/lib/jvm/java-21-openjdk`
4. Click Run ▶️ → Select device/emulator

## Command Line Build

### Setup

**Set ANDROID_HOME:**

```bash
# macOS
export ANDROID_HOME=$HOME/Library/Android/sdk

# Linux
export ANDROID_HOME=$HOME/Android/Sdk

# Windows (PowerShell)
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"

# Windows (CMD)
set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
```

**Create local.properties:**

```bash
# macOS/Linux
echo "sdk.dir=$ANDROID_HOME" > local.properties

# Windows (PowerShell)
"sdk.dir=$env:ANDROID_HOME" | Out-File -FilePath local.properties -Encoding utf8

# Windows (CMD)
echo sdk.dir=%ANDROID_HOME% > local.properties
```

### Build Commands

```bash
# macOS/Linux
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease       # Build release APK
./gradlew test                   # Run tests
./gradlew clean                  # Clean build

# Windows
gradlew.bat assembleDebug
gradlew.bat assembleRelease
gradlew.bat test
gradlew.bat clean
```

**APK location:** `app/build/outputs/apk/debug/app-debug.apk`

### Install on Device

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Enable USB Debugging: Settings → About Phone → Tap "Build Number" 7 times → Developer Options → USB Debugging

## Running Tests

**In Android Studio:**

- Right-click `app/src/test` → Run Tests
- Or use `Ctrl+Shift+F10` (Windows/Linux) / `Cmd+Shift+R` (Mac)

**Command line:**

```bash
./gradlew test                              # All tests
./gradlew test --tests "IntervalFormulaTest"  # Specific test
```

**Windows:** Use `gradlew.bat` instead of `./gradlew`

## Troubleshooting

**Gradle sync issues:**

- Android Studio: File → Invalidate Caches → Invalidate and Restart
- Command line: `./gradlew clean build --refresh-dependencies` (Windows: `gradlew.bat`)

**SDK issues:**

- Install Android SDK Platform 34: Tools → SDK Manager → SDK Platforms → Android 14.0 (API 34)

**Build errors:**

- Set `JAVA_HOME` to JDK 21 installation path
- Verify `local.properties` points to correct SDK location

**Emulator issues:**

- Enable virtualization in BIOS
- Install HAXM (Intel) or Hypervisor (AMD)
