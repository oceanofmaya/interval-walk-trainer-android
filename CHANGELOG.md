# Changelog

## Version 1

### 1.6.0 - 2026-06-12

#### Features

- **Use Health Connect:** Optional Health Connect step and heart-rate summaries for completed workouts, stored locally in workout history. The app reads from Health Connect only — not phone sensors directly — and saves summaries after completion or when Workout History refreshes later.
- **Saved metrics:** Workout detail rows, day summaries, and monthly stats show steps, average heart rate, range, and fast/slow phase averages when data is available. Reopening a day’s Workout Details can backfill values after a watch or tracker syncs, including for older walks.
- **Phase-aware heart rate:** Fast- and slow-phase average heart rates are estimated from your formula timing and the active workout intervals you actually walked.
- **Metrics session:** Active workout timing is tracked across pause, resume, and phone rotation so Health Connect reads use the full workout window.
- **Health Connect reads:** Heart-rate summaries use aggregate reads with paginated sample fallback; step and heart-rate reads are independent so one metric can still save when the other is missing.
- **Permissions:** Use Health Connect can request only missing Health Connect permissions; Workout History can prompt for missing access when metrics are enabled.
- **Post-workout snapshot:** When Health Connect is enabled, the home screen shows a short completion card pointing to Workout Details in History once Health Connect syncs.

#### UI/UX Improvements

- **Workout details:** Per-workout Health Connect lines use clearer labels — `Avg`, `Range 80-112 bpm`, and `Fast (avg)` / `Slow (avg)` — and show `—` placeholders for missing steps, heart rate, and phase values when Health Connect is enabled.
- **Last Workout insight:** Shows formula, duration, and completion time only; saved Health Connect metrics stay in Workout Details.
- **Post-workout snapshot:** Balanced spacing above and below the completion card in the session panel.
- **Settings:** Added a Use Health Connect opt-in; the toggle is disabled when Health Connect is unavailable.
- **FAQ:** Overflow menu and sheet title are now **FAQ**. Questions are grouped under section headings with clearer typography and section dividers instead of lines between every question.
- **Health Connect discovery:** A one-time what’s new sheet on the home screen and a dismissible Workout History banner point users to the FAQ Health Connect section.

#### Bug Fixes

- **Insights:** Fixed home screen insight headings and circle styling disappearing after returning from Workout History.

#### Privacy

- Updated FAQ, README, and Privacy Policy copy for optional Health Connect step and heart-rate reads, including sync timing, placeholders, and where metrics appear. Metrics remain local-only with no GPS, cloud sync, accounts, analytics, or Health Connect writes.

### 1.5.0 - 2026-05-25

#### Features

- **Insight cards:** Three new home screen insight cards — **Current Streak**, **Today**, and **Last Workout** — join the existing Weekly Goal card in the Insights picker.

#### UI/UX Improvements

- **Current Streak card:** Shows consecutive workout days and your personal best; tap to open Workout History.
- **Today card:** Shows today's completed workouts and minutes, or a gentle empty state when none yet today.
- **Last Workout card:** Shows your most recent saved workout formula, duration, and relative time; tap to open that day in Workout History.
- **Insight cards picker:** Helper text now uses neutral copy when all available cards fit under the five-card limit.
- **Insights styling:** Insight cards now use the same surface card treatment as the timer and workout setup cards (no recessed gray background).
- **Insight carousel:** Single-card and multi-card layouts align with other home cards; page gaps appear only while swiping.

#### Chore

- Store screenshots, promo images, and `promo-manifest.json` updated for v1.5.0: home-screen captures stage insight cards per screen (`slow` = none; `home` = Weekly Goal only; `fast` = all four with Weekly Goal in focus; `complete` = all four with Current Streak in focus). Promo 02 and 08 show insight variations visually without Insights callouts.
- Play Store listing copy updated for insight cards (`PLAY_STORE_LISTING.md`).
- **CI:** Upgrade GitHub Actions to Node.js 24-compatible versions (checkout, setup-java, cache, setup-android, action-gh-release).

### 1.4.3 - 2026-05-24

#### Bug Fixes

- **Weekly goal:** Existing users with a saved Monday-start goal setting are now moved to the Sunday-Saturday goal period, matching Workout History and the 1.4.2 behavior for new installs.

### 1.4.2 - 2026-05-24

#### Bug Fixes

- **Weekly goal:** Weeks now start on Sunday so the goal period matches the Workout History calendar and the “Last day” status appears on Saturday instead of Sunday.

### 1.4.1 - 2026-05-24

#### UI/UX Improvements

- **Home screen:** Slightly tighter vertical spacing above the preset selector and between home sections, without changing the session timer or control layout.
- **Picker bottom sheets:** Shared header spacing tokens for insight cards, preset selection, and voice language pickers so choice lists start closer to the sheet title.

### 1.4.0 - 2026-05-24

#### Features

- **Insight cards:** Choose which insight cards appear on the home screen — up to five — from **Settings → Insight cards** or the edit icon beside **Insights**.

#### UI/UX Improvements

- **Insights section:** The home screen **Insights** area now has a distinct section title, an edit control to open the card picker, and a recessed placeholder card when none are selected.
- **Insight cards:** Slightly more compact card layout on the home screen to balance the new Insights header.
- **Weekly goal insight:** The Weekly Goal card remains available in the picker; when selected but no goal is set, it still shows the grayed-out preview with 0/0 workouts and minutes.
- In-app FAQ, README, and store listing copy now refer to the **home screen** instead of the timer screen.
- Store screenshots updated: promo 01 uses the Insights empty placeholder (no cards chosen) so the first frame stays focused on pacing.

### 1.3.7 - 2026-05-24

#### UI/UX Improvements

- **Home screen layout:** Moved the session timer card directly under workout setup so the timer occupies prime screen space; the Weekly Goal insight card moves to the bottom.
- **Overflow menu:** Moved the overflow menu (⋮) to the top-right corner of the timer card, separate from the Reset and Start/Pause controls.
- Store screenshots and promo images updated to reflect the new home screen layout.

### 1.3.6 - 2026-05-23

#### UI/UX Improvements

- **Weekly goal reminders:** Reminder controls in the Weekly Goal editor are disabled when the weekly goal is off or has no targets, matching when reminders can actually fire. Saved reminder preferences are kept and restored when the goal is re-enabled.
- **Weekly goal reminders:** Notification and exact-alarm permission prompts no longer appear when reminders cannot be used.

### 1.3.5 - 2026-05-23

#### UI/UX Improvements

- **Home screen compaction:** Tightened the session timer typography and spacing to reduce vertical scroll on the main screen.
- **Session panel:** Grouped the session timer and workout controls in a bordered card so the active workout area reads as one unit.
- **Overflow menu:** Workout History and Settings moved into the overflow menu (⋮) beside the primary workout controls for thumb-reachable secondary navigation.
- **Weekly goal on home:** The **Weekly Goal** insight card is always shown on the home screen. When a goal is active it uses an icon status badge and radial progress rings; when no goal is set it appears grayed out with 0/0 workouts and minutes as a quick placeholder.
- Store screenshots and promo images updated for the home screen layout, weekly goal insight card, and promo copy.

### 1.3.4 - 2026-05-23

#### UI/UX Improvements

- **Home screen layout:** Reorganized the home screen into grouped sections — a unified workout setup card, an insights carousel for weekly goal progress, the session timer stack, and action controls — with consistent spacing and room for future insight cards.
- **Weekly goal on home:** Replaced the compact chip with a tappable **Weekly Goal** insight card using an icon status badge, radial progress rings, and subdued card styling on the home screen.
- Store screenshots and promo images updated to reflect the redesigned home screen layout.

### 1.3.3 - 2026-05-22

#### UI/UX Improvements

- **Weekly goal reminder days:** Stacked each reminder-day checkbox above its label in the compact grid so day names stay on one line on narrow phones.
- Store screenshots and promo images updated to reflect the weekly goal reminder-days layout change.

#### Chore

- **Screenshot tooling:** Fixed `optimize-screenshots.sh` failing under `sh` with a syntax error when collecting changed PNG files.

### 1.3.2 - 2026-05-22

#### UI/UX Improvements

- **Home screen weekly goal:** Replaced the extra weekly-goal summary line with a tappable compact chip and tightened home-screen spacing so controls stay clear on shorter phones.

### 1.3.1 - 2026-05-22

#### Fixes

- **Workout reminders:** Weekly reminder notifications now use sound and vibration when allowed by the user's device notification settings.

### 1.3.0 - 2026-05-22

#### Features

- **Weekly Goals:** Set a weekly target for workouts, minutes, or both from Workout History. Progress uses only completed workouts saved locally on the device.
- **Goal reminders:** Choose recurring workout reminder days and exact reminder time, with an option to pause reminders once the week’s goal is met.

#### UI/UX Improvements

- **Weekly progress card:** Workout History now shows this week’s progress alongside the existing overview stats.
- **Main-screen status:** The home screen shows a compact “This week” goal summary when weekly goals are enabled.
- Store screenshots and promo images updated for Weekly Goals and reminders.

#### Chore

- **Screenshot tooling:** The screenshot optimization script now optimizes only changed phone screenshots by default, with `--all` available for full-batch optimization.
- **Store listing docs:** Added Play Store listing field constraints and current listing copy to the store assets documentation.

### 1.2.0 - 2026-04-26

#### Features

- **Saved presets:** Save up to 30 custom interval or circuit presets from Design Your Own, pick them under “My saved presets” in the picker, update, rename, duplicate, delete (long-press / overflow menu), and reorder with the drag handle.
- **After Create:** Choose Save & use, Use without saving, or Save only; snackbar confirms Save only.

#### UI/UX Improvements

- **Picker rename:** “Select Training Formula” is now “Select Preset”; sections are “Pre-configured presets” and “My saved presets”. This makes it clearer this is separate from workout history.
- **Sticky “Design Your Own”:** The Design Your Own action is pinned below the list so it stays reachable as you save more presets.
- **Consistent alignment:** Pre-configured rows now left-align like saved-preset rows so the picker reads as one consistent list.
- **Preset row format:** Pre-configured rows now show the name on the first line and the duration + workout type on a second line (matching saved presets); the redundant “(N min)” suffix has been dropped from preset names (e.g. “3-3 Japanese - 5 Rounds”). Existing workout history retains its original formula-name strings.
- **Preset feedback:** Save, update, rename, duplicate and delete now all show a brief confirmation snackbar, and Delete is undoable for ~5 seconds instead of requiring a confirmation dialog. Delete is now a single tap: it removes the preset immediately and exposes an **Undo** action that restores the row with its original position and creation time intact.
- **Active preset rules:** Updating the active preset refreshes the home screen, asks before resetting a running or paused timer, and deleting the active preset converts the current workout to an unsaved custom workout while keeping the timer values.

#### Fixes

- **Timer restore:** Restoring state with `currentInterval == 0` (e.g. rotation during pre-start countdown) no longer leaves the internal interval index negative, which could add an extra slow phase at the end while elapsed time hit the workout cap.

### 1.1.12 - 2026-03-18

#### Fixes

- Fixed elapsed/remaining time drift after restoring interval workouts for both starts-slow and starts-fast formulas.

#### UI/UX Improvements

- Moved the main workout hint to sit below the formula selector in both portrait and landscape.
- Store screenshots and promo images updated.

### 1.1.11 - 2026-03-12

#### UI/UX Improvements

- **Settings version text:** Set version font size in Settings to match the app title (13sp) so they read as one brand block.

### 1.1.10 - 2026-03-11

#### UI/UX Improvements

- **App naming split:** Shortened the launcher label to "Interval Walk" to avoid truncation in app drawers while keeping the full in-app brand name "Interval Walk Trainer".
- **In-app brand visibility:** Settings now shows a subtle top brand row (`Interval Walk Trainer`) with the app version on the next line.
- **Main screen branding:** Added a subtle top brand row on the main screen (`Interval Walk Trainer`) in portrait and landscape layouts.
- Store screenshots and promo images updated.

### 1.1.9 - 2026-03-08

#### Features

- **Voice notifications and media:** Voice announcements (e.g. "Slow walk", "Fast walk", "Workout complete") now request transient audio focus. Music and podcast apps that respect audio focus will pause while the announcement plays and resume when it finishes, so the notification is easier to hear.

#### UI/UX Improvements

- **Vibration cues:** Increased phase-change vibration strength so cues are easier to feel: slow phase uses a stronger moderate pulse, and fast phase uses full amplitude for a clearer “fast walk” cue.

### 1.1.8 - 2026-03-08

#### UI/UX Improvements

- **Settings labels:** Renamed the voice language picker label from "Voice" to "Language" so it’s clear you’re choosing the language for spoken announcements.
- **Keep Screen Awake icon:** Replaced the timer icon with a sun (light mode) icon to better convey “keep screen on / awake”.
- Store screenshots and promo images updated.

### 1.1.7 - 2026-03-07

#### UI/UX Improvements

- **Voice picker bottom sheet:** Disabled dragging so scrolling the language list no longer collapses the sheet; dismiss via Apply or Cancel.

### 1.1.6 - 2026-03-07

#### UI/UX Improvements

- **Settings label polish:** Updated "Sounds & haptics" to "Sounds & Haptics" for consistent title casing.
- **Voice picker flow:** Replaced the voice picker with a bottom sheet that has a single-choice list and sticky Apply/Cancel actions, opens expanded so actions are immediately visible, and lets users dismiss without applying a selection.

### 1.1.5 - 2026-03-07

#### UI/UX Improvements

- **Countdown cue tone:** Refined pre-start "Start" translations in politeness-sensitive languages to use polite/honorific forms for a more respectful voice guidance style.

### 1.1.4 - 2026-03-07

#### UI/UX Improvements

- **Countdown cue copy:** Updated the pre-start cue from a "Go" style to an informal "Start" style across supported locales for a clearer "3, 2, 1, Start" experience in both on-screen text and TTS.

### 1.1.3 - 2026-03-07

#### Chore

- Updated store promo images.

#### Fixes

- **TTS locale resources from Play installs:** Disabled App Bundle language splits so all supported locale string resources are packaged on device, preventing fallback-to-English for selected notification languages that don't match the device language.

### 1.1.2 - 2026-03-07

#### Fixes

- **TTS locale on physical devices:** Fixed locale-specific string resolution returning English text on some OEM devices by using the application context instead of the AppCompat-wrapped Activity context for `createConfigurationContext`.

### 1.1.1 - 2026-03-07

#### Fixes

- **Localized TTS prompts:** Improved locale resolution for voice phrases so announcements like "Go", "Slow walk", "Fast walk", and "Workout complete" correctly use supported language resources on real devices.

#### UI/UX Improvements

- **Voice picker simplification:** The Voice setting now shows a concise, language-level list (one option per supported language) instead of a long raw voice list, while still using an installed device voice behind the scenes.

### 1.1.0 - 2026-03-07

#### Features

- **Voice picker:** You can choose which TTS voice is used for voice notifications. In Settings, tap "Voice" under Workout to pick from available device voices or use the default.
- **TTS in your language:** Voice announcements (slow/fast walk, workout complete, etc.) are spoken in the selected voice’s language when a matching locale is available (e.g. Spanish, French, German, Hindi, Japanese, and many others).
- Store screenshots and promo images updated.

#### UI/UX Improvements

- **Settings grouping:** Workout options are now grouped under "Sounds & haptics," "During Workout," and "History" for a clearer, less cluttered layout.

### 1.0.20 - 2026-03-04

#### UI/UX Improvements

- **Workout History (Monthly Trend):** When a stat has no change vs previous month, show a neutral "−" and "0%" in secondary color so both trend cards keep a consistent badge row and the layout stays balanced.
- **Timer phase labels:** Replaced "> Slow" / ">> Fast" with plain "Slow" and "Fast" for a minimal, balanced look.
- Store screenshots and promo images updated.

### 1.0.19 - 2026-03-01

#### Fixes

- **Circuit workouts**: Corrected duration (5-4-5 × 2 now 28 min), round counter (1/2 vs 2/2), phase transition between rounds (round 2 starts with fast), completion (no trailing slow), and elapsed time on state restore.

### 1.0.18 - 2026-03-01

#### Features

- FAQ (Help): expanded with 12 new Q&A entries to improve user experience and answer common questions.
- FAQ is now a scrollable list (RecyclerView) so future entries can be added easily.

### 1.0.17 - 2026-02-28

#### UI/UX Improvements

- FAQ (Help): align answer text with question text so answers are no longer flush to the edge.
- Adaptive app icon: reduce foreground inset to 4dp for larger icon on store and home screen.

### 1.0.16 - 2026-02-28

#### UI/UX Improvements

- Reduce top padding on main screen now that overflow menu is at the bottom.
- Adaptive app icon: reduce foreground inset (18dp → 8dp) so the icon renders larger on the store and home screen while remaining within the adaptive safe zone.

### 1.0.15 - 2026-02-28

#### Updates

- Upgrade Android Gradle Plugin 8.13.1 → 9.0.1, Gradle 9.0 → 9.3.1, Kotlin 2.0.21 → 2.3.10.
- Upgrade Room 2.7.0 → 2.8.4 and other AndroidX dependencies to latest stable versions.
- Upgrade JUnit Jupiter 5.11.4 → 6.0.3, junit-platform-launcher 1.11.4 → 6.0.3, mockito-kotlin 5.4.0 → 6.2.3.
- Migrate Room from kapt to KSP for faster builds.
- Bump compileSdk and targetSdk to 36.
- Remove `screenOrientation` from manifest and add adaptive layouts (`layout-land/`) for Android 16 compatibility.

#### Chore

- Add `.kotlin/` to `.gitignore` and stop tracking it.
- Fix Kotlin lint: rename Migration parameter `database` to `db` to match supertype.
- Remove unused drawables (`ic_info.xml`, `help.xml`) and strings (`view_stats`, `faq`, `settings_button`).
- Add detekt static analysis with config and baseline; fix WildcardImport, MatchingDeclarationName (rename workout-foreground-service.kt → WorkoutForegroundService.kt), ReturnCount in WorkoutForegroundService; integrate detekt into CI.
- Migrate to AGP 9 built-in Kotlin: remove org.jetbrains.kotlin.android plugin, remove android.builtInKotlin/android.newDsl opt-out; replace applicationVariants with base.archivesName for APK naming.

#### UI/UX Improvements

- Move Workout History and Settings out of the overflow menu into dedicated icon buttons below Reset/Start; overflow now contains Help, Rate App, Report Issue.
- Store screenshot and promo images updated.

### 1.0.14 - 2026-02-27

#### UI/UX Improvements

- Start/Pause button now shows "Resume" when an ongoing workout is paused, clearly distinguishing between starting a new workout and continuing a paused one.

#### Features

- Added "Rate App" to the overflow menu; opens the app's Play Store listing for users to leave a review.

### 1.0.13 - 2026-02-26

#### Fixes

- Fixed duplicate workout entries in history: completing a workout and then rotating the device or restoring the app no longer records the same workout again (recording is now skipped when restoring timer state).
- Clear History now updates total workouts and total minutes in the stats header immediately via an optimistic UI update, so the screen reflects the cleared state without navigating away.

#### UI/UX Improvements

- Workout detail (per-day bottom sheet) now shows the time each workout was completed so multiple sessions on the same day can be distinguished.
- Added the ability to delete individual workouts from history from the workout detail bottom sheet (delete icon per session with confirmation), instead of only clearing all history.

### 1.0.12 - 2026-02-25

#### UI/UX Improvements

- Tightened vertical spacing between the formula selector button and the formula summary.
- Formula summary no longer uses "~" before duration (value is exact, not approximate).
- Formula summary layout adjusted to reduce multiline wrapping on smaller devices: slightly smaller text (12sp), reduced horizontal padding, tighter line spacing, and `breakStrategy="simple"` to prefer keeping the first line full.
- Store screenshot and promo images updated.

#### Chore

- Stopped tracking `.idea/` in git and added it to `.gitignore`.

### 1.0.11 - 2026-02-26

#### UI/UX Improvements

- Replaced main-screen Stats, Settings, Vibration, and Voice buttons with a single overflow menu (⋮) offering Workout History, Settings, Help, and Report Issue, with icons and clearer hierarchy.
- Moved Vibration and Voice notification toggles into Settings under Workout (with switches), keeping the main screen focused on the timer and controls.
- Updated Ux for a cleaner and more spacious layout.
- FAQ is now available from the overflow as **Help**.

#### Features

- Voice notifications now default to **on** (was off).
- **Report Issue** in the overflow opens the app’s GitHub Issues page
- Added a GitHub issue form template for bug reports

#### Updates

- CI: Lint is required for the build
- Updated store screenshots and promo images.

### 1.0.10 - 2026-02-24

#### Updates

- Fixed fast-start interval formulas (including 5-2 and 5-4-5 patterns) to run the final slow phase correctly instead of ending early on the last fast phase.
- Added automatic post-completion timer reset after a short delay (15s) so returning to the app no longer leaves users stuck on the completed 0:00 state.

### 1.0.9 - 2026-02-23

#### UI/UX Improvements

- Added a new Magenta accent swatch in Settings > Accent.

### 1.0.8 - 2026-02-22

#### UI/UX Improvements

- Reworked Settings > Theme controls into compact visual swatches and added accent swatches (Blue, Teal, Purple, Amber) for UI customization.
- Applied accent colors consistently across key interactive UI surfaces, including main controls, settings toggles, custom formula creation controls, and stats highlights.
- Updated phase labeling to use stronger visual differentiation (`>` for slow and `>>` for fast) while keeping a cohesive accent-based color style.
- Updated calendar day styling in stats so the current-day ring is theme-aware for contrast (black in light mode, white in dark mode), regardless of accent selection.

#### Documentation

- Updated README usage and design sections to reflect theme/accent swatches, accent-driven active states, and the latest Settings behavior.

### 1.0.7 - 2026-02-21

#### Features

- Added a Keep Screen Awake setting (default off) that keeps the display on only while the app is in the foreground.
- Added an FAQ entry under Settings > About with a dedicated bottom-sheet accordion experience, including guidance on interval walking, notifications, and background behavior.

### 1.0.6 - 2026-02-20

#### UI/UX Improvements

- Updated Settings bottom sheet layout for better organization and readability.
- Shortened the section heading text from "Workout Settings" to "Workout" in Settings.

### 1.0.5 - 2026-02-19

#### UI/UX Improvements

- Added a Notifications toggle as the first control in Workout Settings, with a dedicated notifications icon and direct system permission/settings flow
- Polished all six active Play Store promo screenshots in the light-theme promo set

### 1.0.4 - 2026-02-18

#### UI/UX Improvements

- Shortened activity-recognition permission messaging to reduce truncation on smaller screens

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
  - 5-4-5 Circuit - 2 Rounds (28 min)
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