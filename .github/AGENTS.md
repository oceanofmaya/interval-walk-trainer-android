# AI Agent Guidelines

When working on this repository, please adhere to the following coding standards and conventions.

## Build Configuration

**CRITICAL**: All code suggestions and implementations must be compatible with the following build configuration:

- **Android Gradle Plugin (AGP)**: 8.13.1s
- **Gradle**: 9.0
- **Java**: 21 (JavaVersion.VERSION_21)
- **Kotlin**: 2.0.21
- **compileSdk**: 34
- **minSdk**: 24 (Android 7.0)
- **targetSdk**: 34

When making code suggestions:

- Use Java 21 language features and APIs
- Ensure Kotlin code is compatible with Kotlin 2.0.21
- Use Android API 34 features when appropriate, but maintain minSdk 24 compatibility
- For API-specific code, use proper version checks: `if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.XXX)`
- When using newer Android APIs (API 33+), provide fallbacks for older versions

## File Naming Conventions

- All markdown files should be named using uppercase letters only.
- All files that are not markdown should be named using lowercase with dashes separating words.

## Branching Strategy

Use `feature/<description>` for all branches. Examples: `feature/add-mushroom-theme`, `feature/v1.0.0`, `feature/fix-division-edge-case`.

Workflow: Create branch from `main` → Make changes → Update version/changelog if releasing → Open pull request → Merge to `main` via PR → GitHub Actions auto-creates tag `vX.Y.Z` from version in `app/build.gradle.kts` → Release workflow builds and publishes release.

**All merges to `main` must occur through pull requests.**

## Commit Messages

Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification as documented in [README.md#contributing](../README.md#contributing). All commit messages must use the conventional commit format with appropriate types (feat, fix, docs, chore, refactor, etc.).

## Linting

All lint errors must be fixed before completing a task. Use `read_lints` to check for lint errors in files you've modified. Lint errors should be resolved immediately after making code changes.

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
