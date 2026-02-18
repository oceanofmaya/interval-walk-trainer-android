# Store Screenshot Guide

This document defines how screenshots are organized for Play Store publishing.

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

## Intent of Each Folder

- `phone/`
  - Keep unstyled, clean UI captures from the app.
  - Use these as editable source assets for future promo iterations.
  - Preserve both themes only when needed for product/design reference.

- `promo/`
  - Keep only phone screenshots intended for Play Store upload.
  - These are manually designed images, composed from the `phone/` captures.
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

Keep these in `promo/` as the active sequence:

1. `01-slow-light-promo.png` (slow interval phase)
2. `02-fast-light-promo.png` (fast interval phase)
3. `03-formula-light-promo.png` (setup and customization entry point)
4. `04-custom-interval-light-promo.png` (design-your-own value)
5. `05-stats-overview-light-promo.png` (progress tracking proof)
6. `06-complete-light-promo.png` (session completion outcome)

This sequence tells a clear story: pace -> intensity -> setup -> personalize -> track -> complete.

## What to Keep vs. Remove in `phone/`

Keep in active source set:

- `formula-light.png`
- `slow-light.png`
- `fast-light.png`
- `custom-interval-light.png`
- `stats-overview-light.png`
- `complete-light.png`
- `home-light.png` (useful fallback hero)
- `splash.png` (branding/reference)

Safe removal candidates:

- Most dark variants if store uploads are light-theme-only
- `settings-*` screenshots (lower marketing value for top 6-8 slots)
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

1. Capture/update raw source screenshots in `phone/`.
2. Rebuild affected promo images manually.
3. Export final images to `promo/` using the `[position]-[screen]-light-promo.png` pattern.
4. Update `promo-manifest.json` with source mapping, feature intent, and exact caption text.
5. Optionally optimize all screenshots:

```bash
./scripts/optimize-screenshots.sh
```

1. Verify Play Store constraints before upload:
   - 2-8 phone screenshots
   - 16:9 or 9:16 ratio
   - 320px min, 3840px max

## Current Simplifications

- Add tablet folders only when those listings are needed:
  - `assets/store/screenshots/tablet-7/`
  - `assets/store/screenshots/tablet-10/`
