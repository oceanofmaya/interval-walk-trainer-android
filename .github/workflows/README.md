# GitHub Actions Workflows

This directory contains automated workflows for building, testing, and releasing the Interval Walk Trainer app.

## Available Workflows

### 1. Build and Test (`build.yml`)

**Triggers:**

- Push to `main` branch
- Pull Requests targeting `main`

**What it does:**

- Runs unit tests to verify code quality
- Runs lint checks (non-blocking)
- Builds debug APK to verify compilation

**Purpose:** Ensures code can be built and tests pass before merging

**Manual trigger:** No, automatic on push/PR

### 2. Tag Release (`tag-release.yml`)

**Triggers:** Push to `main` branch when `app/build.gradle.kts` or `CHANGELOG.md` changes

**What it does:**

- Extracts version from `app/build.gradle.kts`
- Checks if tag already exists
- Creates and pushes version tag (e.g., `v1.0.0`, `v1.0.0-beta.1`) if it doesn't exist

**Purpose:** Automatically creates version tags when version is updated and merged to `main`

**Manual trigger:** No, automatic on version changes

### 3. Release (`release.yml`)

**Triggers:** Push of version tag matching `v*.*.*` (e.g., `v1.0.0`)

**What it does:**

- Verifies tag format is correct (vX.Y.Z)
- Verifies version in `app/build.gradle.kts` matches the tag
- Runs unit tests to ensure release quality
- Runs lint checks
- Builds release APK and AAB (Android App Bundle)
- Verifies artifacts were created successfully
- Creates GitHub Release with both APK and AAB artifacts

**Purpose:** Automated release creation when a version tag is pushed

**Manual trigger:** No, automatic on tag push

## Workflow Summary

| Workflow | Trigger | Tests | Lint | Builds | Output |
|----------|---------|-------|------|--------|--------|
| Build and Test | Push/PR | ✅ | ⚠️ | Debug APK | Logs only |
| Tag Release | Version change on main | ❌ | ❌ | ❌ | Creates tag |
| Release | Tag push | ✅ | ✅ | Release APK + AAB | GitHub Release |

## Typical Workflow

### Development

1. Create feature branch: `git checkout -b feature/your-feature`
2. Make changes and commit
3. Push to feature branch → Build workflow runs automatically
4. Open pull request → Build workflow runs again
5. Review test results and build artifacts
6. Merge PR to `main` → Build workflow runs final verification

### Release Process

1. **Update version and changelog:**

```bash
# Update version in app/build.gradle.kts
# Update CHANGELOG.md with release notes
git add app/build.gradle.kts CHANGELOG.md
git commit -m "chore: bump version to 1.0.0"
git push origin main
```

1. **Tag Release workflow automatically:**
   - Detects version change in `app/build.gradle.kts`
   - Creates and pushes tag `v1.0.0` automatically

2. **Release workflow automatically:**

   - Verifies version matches build.gradle.kts
   - Runs all tests
   - Runs lint checks
   - Builds release APK and AAB
   - Creates GitHub Release with artifacts

3. **Manual Play Store Publishing:**

   - Go to the GitHub Release page
   - Download the `app-release.aab` file
   - Upload to Google Play Console manually
   - Follow Play Store's standard publishing process

## Key Features

- **Automated Testing:** Tests run on every push and PR
- **Version Validation:** Release workflow ensures tag version matches build configuration
- **Quality Checks:** Lint checks catch potential issues before release
- **Artifact Verification:** Release workflow verifies artifacts exist before creating release
- **Gradle Caching:** Dependencies are cached for faster builds
- **Comprehensive Logging:** Clear error messages help debug issues

## Notes

- All workflows use JDK 21 and Android SDK as specified in BUILD.md
- Release builds (APK/AAB) are only created in the release workflow when you push a tag
- Release APKs require signing configuration (set up before creating your first release)
- Debug builds don't require signing and are used for development/testing
- Play Store publishing is done manually via Google Play Console
- Test results are visible in workflow logs (no artifacts uploaded for public repo security)
