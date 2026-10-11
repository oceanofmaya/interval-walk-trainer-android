import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
}

android {
    namespace = "com.oceanofmaya.intervalwalktrainer.wear"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.oceanofmaya.intervalwalktrainer"
        minSdk = 30
        targetSdk = 36
        // Keep Wear artifacts in a distinct Play version-code range.
        versionCode = 76_001
        versionName = "1.8.0"
    }

    signingConfigs {
        create("release") {
            val phoneAppDirectory = rootProject.file("app")
            val keystorePropertiesFile = phoneAppDirectory.resolve("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val properties = Properties().apply {
                    load(FileInputStream(keystorePropertiesFile))
                }
                storeFile = phoneAppDirectory.resolve(properties["storeFile"] as String)
                storePassword = properties["storePassword"] as String
                keyAlias = properties["keyAlias"] as String
                keyPassword = properties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.activity:activity-ktx:1.12.2")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.wear:wear:1.3.0")
    implementation("com.google.android.gms:play-services-wearable:19.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}
