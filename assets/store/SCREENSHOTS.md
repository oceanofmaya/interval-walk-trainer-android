# Store Screenshot Guide

This document defines how screenshots are organized for Play Store publishing. For Google Play listing text, field limits, and current store copy, see  
`[PLAY_STORE_LISTING.md](PLAY_STORE_LISTING.md)`.

## Folder Structure

```text
assets/store/screenshots/
├── phone/   # Raw emulator captures (source of truth)
├── promo/   # Final manual promo compositions for Play Store (phone)
└── promo-manifest.json   # Source -> promo -> message mapping
```

`promo-manifest.json` stores two different content layers:

- `feature_message`: internal product intent for each screenshot
- `caption`: exact text rendered on the promo image (`headline`, `supporting_text`, and optional `callouts`)

## Promo Manifest Copy Guidelines

Treat `promo-manifest.json` as both a source map and a marketing-copy plan. The
copy is rendered into Play Store promo images, so it should help a browsing user
quickly understand why the app is useful.

- `feature_message`
  - Internal intent for the screenshot.
  - Can be descriptive, complete, and more product/engineering oriented.
  - Does not need to be suitable as rendered ad copy.
- `headline`
  - Short emotional hook.
  - Should be easy to scan at the top of the promo image.
  - Prefer benefit-led phrasing over feature inventory.
- `supporting_text`
  - Rendered under the headline.
  - Should explain the benefit or value proposition in a concise, user-facing way.
  - Avoid overly technical wording unless the technical detail is itself a selling point.
- `callouts`
  - Optional short bubbles placed on top of the screenshot.
  - Use them to point to visual details, locate features mentioned in the supporting text, or clarify something visible but not obvious.
  - Repeating a phrase from `supporting_text` is acceptable when the callout helps the viewer find that feature in the image.
  - Avoid callouts that merely repeat the supporting text without adding visual context.
  - Keep callouts short because they cover part of the screenshot.
  - Empty callouts are acceptable and often preferred when the screenshot and caption already communicate the message.

Good callout examples:

- A "Clear Phase Cues" bubble near the phase label when the supporting text mentions phase cues.
- A "Pick notification language" bubble near the language row because the row is visible but not immediately obvious.
- Theme/accent option bubbles near the visible swatches.

Avoid:

- Turning callouts into a checklist of every feature.
- Adding long callouts that hide important UI.
- Repeating a headline/supporting-text phrase when it does not help interpret the image.

## Intent of Each Folder

- `phone/`
  - Keep unstyled, clean UI captures from the app.
  - Use these as editable source assets for future promo iterations.
  - Preserve both themes only when needed for product/design reference.
- `promo/`
  - Keep only phone screenshots intended for Play Store upload.
  - These are manually designed images, composed using the `phone/` captures.
  - This folder is the canonical upload set for phone listings.

## Naming Convention

- Raw phone captures:
  - `[screen]-[theme].png` (example: `formula-light.png`)
  - `splash.png` for launch screen
- Promo images:
  - `[position]-[screen]-light-promo.png` (example: `03-formula-light-promo.png`)
  - Use zero-padded position (`01`-`08`) to lock upload order
  - Keep `-promo` suffix consistently for publish-ready composites

## Current Phone Play Store Set (Recommended Keep)

Google Play allows up to 8 phone screenshots. Keep these in `promo/` as the active sequence:

1. `01-slow-light-promo.png` (slow interval phase with Workout Metrics setup and Weekly Goal insight)
2. `02-fast-light-promo.png` (fast interval phase with Last Workout metrics insight)
3. `03-formula-light-promo.png` (setup and customization entry point)
4. `04-custom-interval-light-promo.png` (design-your-own value)
5. `05-stats-overview-light-promo.png` (progress tracking and motivation proof)
6. `06-settings-theme-accent-light-promo.png` (theme + accent personalization)
7. `07-workout-details-metrics-light-promo.png` (workout details with Health Connect metrics)
8. `08-complete-light-promo.png` (session completion outcome)

This sequence tells a clear story: pace + metrics setup -> intensity + last workout metrics -> setup -> personalize workout -> track progress -> personalize look -> detailed workout metrics -> complete.

## Insights in Phone Captures

Home-screen sources (`slow-light`, `fast-light`, `complete-light`) include the **Insights** section at the bottom of the home screen. Before capturing, stage insight cards in **Settings → Insight cards** (or the edit icon beside **Insights**).

| Source | Promo | Insight cards enabled | Carousel / focus |
| --- | --- | --- | --- |
| `slow-light.png` | 01 | **All four** (Weekly Goal, Current Streak, Today, Last Workout) plus the home **Workout Metrics** enable card visible above Insights | **Weekly Goal** snapped in view |
| `fast-light.png` | 02 | **All four** (Weekly Goal, Current Streak, Today, Last Workout) | **Last Workout** snapped in view with saved steps and average heart rate |
| `complete-light.png` | 08 | **All four** | **Current Streak** snapped in view |

**Promo subtlety:** Promos 02 and 08 demo insight-card UX variations through the screenshot itself — carousel page choice and enabled cards — without adding Insights-specific callouts. Existing callouts on promo 02 remain workout-focused (phase cues, vibration).

- **Promo 07 (`workout-details-metrics-light`):** Shows a completed day’s Workout Details sheet with Health Connect steps, average heart rate, and fast-vs-slow phase metrics.

When rebuilding promo images for a release that touches Insights, update both `phone/` captures (with the staging above) and matching `promo/` composites, then sync `promo-manifest.json`.

## What to Keep vs. Remove in `phone/`

Keep in active promo source set:

- `formula-light.png`
- `slow-light.png`
- `fast-light.png`
- `custom-interval-light.png`
- `stats-overview-light.png`
- `settings-theme-accent-light.png`
- `workout-details-metrics-light.png`
- `complete-light.png`
- `splash.png` (branding/reference)

Keep as retained source/reference captures:

- `weekly-goal-reminders-light.png` (weekly goal and reminder setup; not in the active 8-promo set)

Safe removal candidates:

- Most dark variants if store uploads are light-theme-only
- `custom-circuit-*` if current message focuses on interval workflow
- `stats-calendar-*` if `stats-overview-*` already communicates outcomes

## Manual Promo Nuance (Important)

Promo images in `promo/` are manually created from `phone/` screenshots. To keep this reproducible:

- Treat `phone/` captures as source material.
- Preserve one source file for each promo file.
- Maintain visual consistency in overlays (text style, spacing, gradient, CTA language).
- Update promo images when core UI changes, feature names change, or branding changes.
- Keep `assets/store/screenshots/promo-manifest.json` updated when adding, renaming, or replacing promo images.

## Update Workflow

1. Stage insight cards and carousel focus per [Insights in Phone Captures](#insights-in-phone-captures) when home-screen sources change.
2. Capture/update raw source screenshots in `phone/`.
3. Rebuild affected promo images manually.
4. Export final images to `promo/` using the `[position]-[screen]-light-promo.png` pattern.
5. Update `promo-manifest.json` with source mapping, feature intent, and exact caption text.
6. Optionally optimize changed screenshots:

```bash
./scripts/optimize-screenshots.sh
```

By default, the script optimizes only changed, staged, or untracked PNG files in
`assets/store/screenshots/phone/` as reported by Git. To optimize every phone
screenshot, run:

```bash
./scripts/optimize-screenshots.sh --all
```

7. Verify Play Store constraints before upload:
  - 2-8 phone screenshots
  - 8 screenshots is the maximum; adding a new promo requires replacing or removing an existing one
  - 16:9 or 9:16 ratio
  - 320px min, 3840px max

## Current Simplifications

- Add tablet folders only when those listings are needed:
  - `assets/store/screenshots/tablet-7/`
  - `assets/store/screenshots/tablet-10/`

