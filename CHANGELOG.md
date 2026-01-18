# Changelog

## Version 1

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
