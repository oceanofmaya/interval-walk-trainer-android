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
- **On `main` branch only:** If version files changed and tests passed:
  - Automatically creates and pushes version tag
  - Builds signed release APK and AAB
  - Creates GitHub Release with artifacts

**Purpose:** Ensures code can be built and tests pass before merging. Automatically creates tags and releases after successful build/test on main.

**Manual trigger:** No, automatic on push/PR

## Workflow Summary

| Workflow | Trigger | Tests | Lint | Builds | Output |
|----------|---------|-------|------|--------|--------|
| Build and Test | Push/PR | ✅ | ⚠️ | Debug APK + Release (on main) | Logs + Tag + GitHub Release (on main) |

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

1. **Build and Test workflow automatically:**
   - Runs tests and builds
   - If tests pass and version changed on `main`:
     - Creates and pushes tag `v1.0.0` automatically
     - Builds signed release APK and AAB
     - Creates GitHub Release with artifacts

3. **Manual Play Store Publishing:**

   - Go to the GitHub Release page
   - Download the `app-release.aab` file
   - Upload to Google Play Console manually
   - Follow Play Store's standard publishing process

## Key Features

- **Automated Testing:** Tests run on every push and PR
- **Version Validation:** Build workflow ensures tag version matches build configuration
- **Quality Checks:** Lint checks catch potential issues before release
- **Artifact Verification:** Build workflow verifies artifacts exist before creating release
- **Gradle Caching:** Dependencies are cached for faster builds
- **Comprehensive Logging:** Clear error messages help debug issues

## Notes

- All workflows use JDK 21 and Android SDK as specified in BUILD.md
- Release builds (APK/AAB) are only created in the build workflow when version changes on main
- Release APKs require signing configuration (set up before creating your first release)
- Debug builds don't require signing and are used for development/testing
- Play Store publishing is done manually via Google Play Console
- Test results are visible in workflow logs (no artifacts uploaded for public repo security)
