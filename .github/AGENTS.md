# AI Agent Guidelines

When working on this repository, please adhere to the following coding standards and conventions.

## Build Configuration

**CRITICAL**: All code suggestions and implementations must be compatible with the following build configuration:

- **Android Gradle Plugin (AGP)**: 9.0.1
- **Gradle**: 9.3.1
- **Java**: 21 (JavaVersion.VERSION_21)
- **Kotlin**: 2.3.10
- **compileSdk**: 36
- **minSdk**: 24 (Android 7.0)
- **targetSdk**: 36

When making code suggestions:

- Use Java 21 language features and APIs
- Ensure Kotlin code is compatible with Kotlin 2.3.10
- Use Android API 36 features when appropriate, but maintain minSdk 24 compatibility
- For API-specific code, use proper version checks: `if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.XXX)`
- When using newer Android APIs (API 33+), provide fallbacks for older versions

## File Naming Conventions

- All markdown files should be named using uppercase letters only.
- All files that are not markdown should be named using lowercase with dashes separating words.
- Android drawable icon resources should follow the source/style naming convention used by the Android/Material icon library:
  - Use `baseline_<icon_name>_24.xml` for baseline Material icons.
  - Use `outline_<icon_name>_24.xml` for outlined Material icons.
  - Reserve `ic_*` names for launcher icons or truly app-specific/custom icon assets that do not map cleanly to a Material icon style/name.
  - Do not introduce one-off icon names when a Material-library style/name exists; use Android Studio Refactor Rename when normalizing existing drawables so all references are updated safely.

## Branching Strategy

Use `feature/<description>` for all branches. Examples: `feature/add-mushroom-theme`, `feature/v1.0.0`, `feature/fix-division-edge-case`.

Workflow: Create branch from `main` → Make changes → Update version/changelog if releasing → Open pull request → Merge to `main` via PR → GitHub Actions auto-creates tag `vX.Y.Z` from version in `app/build.gradle.kts` → Release workflow builds and publishes release.

**All changes must be made on a `feature/*` branch (never commit directly to `main`).**

**All merges to `main` must occur through pull requests, and all required PR checks must pass before merge.**

## Commit Messages

Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification.

For this repository's commit message policy (including `release`/`beta` scope conventions and examples), follow [`CONTRIBUTING.md`](../CONTRIBUTING.md).

## Linting

All lint errors must be fixed before completing a task. Use `read_lints` to check for lint errors in files you've modified. Lint errors should be resolved immediately after making code changes.

- **Android Lint:** `./gradlew lint`
- **detekt:** `./gradlew detekt` — Kotlin static analysis. Config: `config/detekt/detekt.yml`. Baseline: `config/detekt/baseline.xml`. To add new findings to the baseline: `./gradlew detektBaseline`

## Versioning and Changelog

All changes merged into this repository must include:

1. **Changelog Entry**: Update `CHANGELOG.md` with a new version entry following this pattern:
  - Major version grouping: `## Version X` (if creating a new major version, otherwise add to existing major version section)
  - Version heading: `### X.Y.Z - YYYY-MM-DD` (placed under the appropriate major version section)
  - Category headings: Use `#### Category` for organizing changes (e.g., `Breaking Changes`, `Updates`, `UI/UX Improvements`, `Chore`, `Features`, etc.)
  - List items under each category describing the changes
2. **Version Update**: Update the version in all locations to match the version in `CHANGELOG.md` following [semantic versioning](https://semver.org/):
  - Update `versionName` in `app/build.gradle.kts` to match the semantic version (e.g., `1.0.0`, `1.1.0`, `2.0.0`)
  - **Increment `versionCode`** in `app/build.gradle.kts` - this integer must be incremented for each release, regardless of the semantic version change. The `versionCode` is used by the Google Play Store to determine which version is newer and must always increase with each release.

## Documentation and user-facing copy

When the app’s behavior changes, update the following so they stay accurate:

- **README.md**: Adjust the Features, Usage, Training Formulas, and any other sections that describe how the app works or how to use it.
- **In-app FAQ (Help)**: The FAQ is built from strings in `app/src/main/res/values/strings.xml` (e.g. `faq_question_*`, `faq_answer_*`) and the list in `MainActivity.kt` (`faqEntries`). Add, remove, or edit Q&A entries so the Help bottom sheet reflects current behavior.

## Android UI/UX Conventions

When adding or changing app UI, first inspect nearby screens and existing bottom sheets for established patterns, then match those patterns unless there is a clear product reason to diverge.

- Use the app's existing theme colors and accent behavior for interactive controls. Buttons, switches, checkboxes, icons, and selected states should use the same accent/tint patterns already used elsewhere in the app, rather than default Material colors.
- When adding or changing icons, check that drawable names follow the `baseline_*_24` / `outline_*_24` convention for Material icons, and that icon tinting matches nearby UI (`@color/text_secondary`, theme accent, or another established local pattern).
- Build bottom sheets as cohesive workflows. Group related settings together, order controls by how users think about the task, and keep supporting options close to the feature toggle or section they modify.
- For bottom sheets, follow the sizing approach in `MainActivity.configureBottomSheet`: set content width to `MATCH_PARENT`, force the Material `design_bottom_sheet` container to `MATCH_PARENT` on show when needed, and measure content using the resolved parent width with an `EXACTLY` width spec. Avoid `AT_MOST` width measurement for sheet content, since it can collapse labels to only a few characters.
- For rows with trailing controls, prefer the existing row pattern: a full-width clickable row, a label using `layout_width="0dp"` and `layout_weight="1"`, and a fixed-size control aligned at the end. Avoid overlaying labels and controls in a `FrameLayout` when a horizontal weighted row communicates the layout better.
- Use compact visible labels only when they improve readability or fit, and keep full labels available through content descriptions or surrounding context for accessibility.
