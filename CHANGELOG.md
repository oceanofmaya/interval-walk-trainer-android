# Changelog

## Version 1

### 1.0.3 - 2026-02-18

#### Updates

- Added a dedicated workout foreground service to keep interval sessions alive while the app is backgrounded or the screen is off
- Added Android health foreground-service declarations and required permissions for modern background workout execution
- Upgraded UI support dependencies (`androidx.appcompat` and Material Components) to newer stable versions

#### UI/UX Improvements

- Updated Play Store promo screenshots with refreshed visuals across the six active light-theme promo assets

#### Documentation

- Updated privacy policy to document foreground-service and physical-activity permission behavior, including explicit on-device-only data handling

### 1.0.2 - 2026-02-17

#### UI/UX Improvements

- Reworked Play Store phone screenshots into a curated six-image promo sequence with numbered ordering for consistent upload flow
- Removed lower-priority and duplicate phone screenshots to keep only the active light-theme source set used for promo composition

#### Documentation

- Updated README screenshot section to use the new ordered promo assets
- Added `assets/store/SCREENSHOTS.md` to document screenshot structure, naming, maintenance workflow, and source-to-promo process
- Added `assets/store/screenshots/promo-manifest.json` to track promo ordering, source mapping, feature intent, and exact rendered caption copy

#### Chore

- Removed `assets/store/promotional-graphic.png` because it was tied to legacy Android promotional graphic requirements that are no longer supported

### 1.0.1 - 2026-02-14

#### UI/UX Improvements

- Updated app icon to use the new hand-designed logo across all density buckets
- Added splash screen showing the app logo on launch (supports both light and dark themes)
- Added legacy launcher icon support for pre-API 26 devices

#### Chore

- Replaced generated store assets (app icon, feature graphic, promotional graphic) with manually designed versions
- Removed asset generation scripts in favor of manually designed assets
- Added AndroidX SplashScreen dependency for backward-compatible splash screen support

### 1.0.0 - 2025-02-03

#### Production Release

- **Interval Walk Trainer 1.0.0** — first stable production release.
- Pre-configured formulas (3-3 Japanese, 5-2 High Intensity, 5-4-5 Circuit), custom interval/circuit builder, workout tracking with progress and phase colors, stats and calendar history, notifications (voice + haptics), optional pre-start countdown, theme support (system/light/dark), and optional workout saving — all refined through the beta period and ready for production use.

### 1.0.0-beta.22 - 2026-02-01

#### Chore

- Reworked edge-to-edge setup to avoid deprecated Android 15 APIs

### 1.0.0-beta.21 - 2026-01-31

#### Documentation

- Updated README with pre-start countdown feature documentation
- Updated Terms and Conditions references to Terms of Service

### 1.0.0-beta.20 - 2026-01-31

#### Chore

- Updated edge-to-edge handling for modern system insets

### 1.0.0-beta.19 - 2026-01-25

#### Features

- Added pre-start countdown with voice and haptic cues (configurable in settings)

#### UI/UX Improvements

- Improved settings layout and toggle controls
- Updated Terms of Service

### 1.0.0-beta.18 - 2026-01-25

#### UI/UX Improvements

- Tidied settings headings for consistency

### 1.0.0-beta.17 - 2026-01-25

#### UI/UX Improvements

- Made all settings actions use the same text button style for consistent UI

### 1.0.0-beta.16 - 2026-01-25

#### Bug Fixes

- Fixed settings bottom sheet content being obscured by gesture navigation bar when scrolled to the bottom
- Applied navigation bar insets to the settings sheet scroll view so the last buttons remain fully visible

### 1.0.0-beta.15 - 2026-01-25

#### Bug Fixes

- Fixed settings bottom sheet content being hidden behind system navigation bar on devices with gesture navigation
- Added window insets handling to bottom sheet to account for system navigation bar overlay
- Improved bottom padding to ensure last button remains visible when scrolling

### 1.0.0-beta.14 - 2026-01-25

#### Bug Fixes

- Fixed settings bottom sheet collapsing and cutting off bottom buttons on smaller phones
- Increased bottom padding in settings sheet to ensure all buttons remain visible
- Improved peek height calculation to always show full content when possible

### 1.0.0-beta.13 - 2026-01-25

#### UI/UX Improvements

- Improved button text visibility on smaller screens
- Shortened button labels for better readability ("Clear History", "Save Workouts")
- Fixed text wrapping in settings buttons

### 1.0.0-beta.12 - 2026-01-21

#### Features

- Added system theme integration - app automatically follows device theme (default)
- Added theme selector in settings (System, Light, Dark) with intuitive icons
- Added settings dialog accessible via the settings button
- Added Privacy Policy access from settings
- Added Terms and Conditions with medical disclaimer
- Added workout stats section in settings with clear all stats option
- Added workout saving preference toggle (default: enabled)

#### UI/UX Improvements

- Removed coach marks for cleaner onboarding experience
- Removed theme toggle from home screen (now accessible via settings)
- Settings screen provides comprehensive access to app configuration and legal documents

#### Updates

- System theme is now the default theme mode
- Workout recording respects user preference (can be disabled in settings)
- Added confirmation before disabling workout history saving

### 1.0.0-beta.11 - 2026-01-21

#### Updates

- Added a non-destructive database migration to preserve workout history

### 1.0.0-beta.10 - 2026-01-21

#### UI/UX Improvements

- Adjusted bottom sheet peek range to 40-80% for better content visibility

### 1.0.0-beta.9 - 2026-01-21

#### UI/UX Improvements

- Added one-time coach marks for custom formula and circuit mode
- Added a hint for voice cues
- Adjusted bottom sheet peek minimum to 50% of screen height for better usability

### 1.0.0-beta.8 - 2026-01-20

#### UI/UX Improvements

- Enhanced bottom sheets with draggable peek states and content-aware heights
- Fixed workout detail labels/values truncating in the summary cards

### 1.0.0-beta.7 - 2026-01-19

#### Build Improvements

- Release artifacts now include version in filename (e.g., `intervalwalktrainer-1.0.0-beta.7.aab`)

### 1.0.0-beta.6 - 2026-01-18

#### UI/UX Improvements

- Added subtle phase transition animations for a more polished feel
- Added a lightweight confetti overlay on workout completion
- Enabled edge-to-edge layouts with dynamic insets across screens
- Tuned haptics by action type for more consistent feedback
- Enabled dynamic color (Material You) with contrast enforcement

### 1.0.0-beta.5 - 2026-01-18

#### Bug Fixes

- Fixed vibration not triggering on workout completion
- Improved vibration reliability across all phase transitions

### 1.0.0-beta.4 - 2026-01-18

#### UI/UX Improvements

- Added haptic feedback to all button taps across the app for tactile response
- Added safe area padding above top navigation bar to prevent content overlap with status bar on edge-to-edge screens
- Fixed circuit pattern radio button text overflow on smaller screens

### 1.0.0-beta.3 - 2026-01-17

#### UI/UX Improvements

- Fixed adaptive icon padding to prevent edge cutoff on Android launchers
- Improved icon visibility across different device mask shapes

### 1.0.0-beta.2 - 2026-01-17

#### Changes

- Updated target SDK to API level 35 (Android 15) for Play Store compliance

### 1.0.0-beta.1 - 2025-01-17

#### Beta Release

- Initial beta release for internal testing
- This is a draft release for testing purposes

#### Features

- **Three pre-configured training formulas**
  - 3-3 Japanese - 5 Rounds (30 min) - Default
  - 5-2 High Intensity - 4 Rounds (28 min)
  - 5-4-5 Circuit - 2 Rounds (36 min)

- **Design Your Own**: Create custom interval or circuit training formulas
  - Adjustable slow and fast durations (1-60 minutes)
  - Adjustable rounds (1-100)
  - Choose to start with slow or fast phase
  - Circuit mode with three-phase patterns
  - Custom formulas are saved automatically

- **Workout tracking**
  - Visual progress bar showing overall completion
  - Elapsed and remaining time displays
  - Current interval counter
  - Progress bar color changes with phase (blue for slow, red for fast)
  - Automatic workout recording when workouts complete

- **Workout statistics and history**
  - Calendar view with workout day indicators
  - Today indicator (outline ring) and workout days (filled circle)
  - Monthly navigation (previous/next month)
  - Statistics display: total workouts, total minutes, current streak, longest streak, average workouts per week
  - Clear all stats functionality

- **Notifications**
  - Vibration patterns: gentle for slow phase, strong for fast phase
  - Voice announcements with text-to-speech
  - Both can be toggled on/off

- **Theme support**
  - Light and dark themes
  - Theme preference is saved
  - Smooth transitions between themes

- **Background operation**
  - Timer continues running when phone is locked
  - Accurate timing even when screen is off

- **Minimalist interface**
  - Large, readable timer display
  - Clean icon-based controls
  - Color-blind accessible design (blue/red color coding)
