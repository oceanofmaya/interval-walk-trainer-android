# App Signing Setup Guide

This guide explains how to set up signing for release builds of the Interval Walk Trainer app.

## Step 1: Create a Keystore

Create a keystore file using the `keytool` command (comes with JDK):

```bash
keytool -genkey -v -keystore app/keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias interval-walk-trainer
```

You'll be prompted for:

- **Keystore password**: Choose a strong password (save this securely!)
- **Key password**: Press Enter to use the same password as keystore, or enter a different one
- **Your name**: Your name or organization name
- **Organizational Unit**: (optional)
- **Organization**: (optional)
- **City**: (optional)
- **State**: (optional)
- **Country code**: Two-letter country code (e.g., US)

**Important**:

- The keystore file (`app/keystore.jks`) is already in `.gitignore` - **DO NOT commit it to git**
- Save the passwords securely (password manager, secure note, etc.)
- **Losing the keystore means you cannot update your app on Play Store** - make backups!

## Step 2: Create keystore.properties File

Create a file `app/keystore.properties` (this will be gitignored):

```properties
storePassword=your-keystore-password-here
keyPassword=your-key-password-here
keyAlias=interval-walk-trainer
storeFile=keystore.jks
```

**Important**: This file contains passwords and should NOT be committed to git. We'll add it to `.gitignore`.

## Step 3: Update build.gradle.kts

Add signing configuration to `app/build.gradle.kts`:

```kotlin
android {
    // ... existing configuration ...
    
    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("app/keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = java.util.Properties()
                keystoreProperties.load(java.io.FileInputStream(keystorePropertiesFile))
                
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

## Step 4: Update .gitignore

Ensure `keystore.properties` is in `.gitignore`

## Step 5: Test Locally

Build a release APK to verify signing works:

```bash
./gradlew assembleRelease
```

The signed APK will be at: `app/build/outputs/apk/release/app-release.apk`

## Step 6: Setup for GitHub Actions (CI/CD)

For the release workflow to work, you need to store the signing credentials as GitHub Secrets:

1. Go to your GitHub repository
2. Settings → Secrets and variables → Actions
3. Add the following secrets:
   - `KEYSTORE_PASSWORD`: Your keystore password
   - `KEY_PASSWORD`: Your key password (if different, otherwise same as keystore)
   - `KEYSTORE_BASE64`: Base64-encoded keystore file (see below)

### Creating KEYSTORE_BASE64 Secret

Encode your keystore file to base64:

```bash
# macOS/Linux
base64 -i app/keystore.jks | pbcopy

# Or save to file
base64 -i app/keystore.jks > keystore-base64.txt
```

Then copy the output and paste it as the `KEYSTORE_BASE64` secret value.

## Step 7: Update Release Workflow

The release workflow will need to be updated to:

1. Decode the keystore from the secret
2. Create keystore.properties from secrets
3. Build signed release APK/AAB

## Backup Your Keystore

**CRITICAL**: Make secure backups of:

- `app/keystore.jks` file
- Keystore password
- Key password
- Key alias name

Store backups in multiple secure locations (encrypted cloud storage, password manager, etc.)

**If you lose the keystore, you cannot update your app on Google Play Store!**

## Troubleshooting

### "Keystore file not found"

- Ensure `keystore.jks` is in the `app/` directory
- Check the path in `keystore.properties`

### "Keystore was tampered with, or password was incorrect"

- Verify the passwords in `keystore.properties` are correct
- Ensure no extra spaces or newlines in the password

### "Cannot recover key"

- Verify the key alias matches what's in `keystore.properties`
- Check that the key password is correct
