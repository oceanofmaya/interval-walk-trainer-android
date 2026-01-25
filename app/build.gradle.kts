import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("jacoco")
}

android {
    namespace = "com.oceanofmaya.intervalwalktrainer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.oceanofmaya.intervalwalktrainer"
        minSdk = 24
        targetSdk = 35
        versionCode = 13
        versionName = "1.0.0-beta.13"
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = project.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
                
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

    applicationVariants.all {
        val variant = this
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val appName = "intervalwalktrainer"
            val extension = output.outputFileName.substringAfterLast('.')
            output.outputFileName = "$appName-${variant.versionName}.$extension"
        }
    }

    // Rename AAB files after bundle task completes
    tasks.whenTaskAdded {
        if (name.startsWith("bundle") && name.endsWith("Release")) {
            doLast {
                val bundleDir = file("${layout.buildDirectory.get()}/outputs/bundle/release")
                bundleDir.listFiles()?.filter { it.extension == "aab" }?.forEach { aab ->
                    val versionName = android.defaultConfig.versionName
                    val newName = "intervalwalktrainer-$versionName.aab"
                    if (aab.name != newName) {
                        aab.renameTo(File(bundleDir, newName))
                    }
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        viewBinding = true
    }
    
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
    
    // Configure JUnit 5 for unit tests
    tasks.withType<Test> {
        useJUnitPlatform()
    }
    
    // Configure JaCoCo to exclude Robolectric and Android framework classes from instrumentation
    // This prevents VerifyError when Robolectric manipulates bytecode that JaCoCo has instrumented
    // NOTE: Android Studio's built-in coverage tool may not respect these settings.
    // For Android Studio coverage, configure exclusions in Run → Edit Configurations → Code Coverage
    tasks.withType<Test> {
        configure<JacocoTaskExtension> {
            // Exclude Robolectric and Android framework classes from coverage reporting
            excludes = listOf(
                "jdk.internal.*",
                "org.robolectric.*",
                "android.**",
                "androidx.**",
                "com.android.**"
            )
        }
    }
    
    // Configure JaCoCo agent with excludes to prevent instrumentation of Android framework classes
    // This is done via the jacoco extension configuration
    afterEvaluate {
        tasks.withType<Test> {
            val jacocoExtension = extensions.findByType<JacocoTaskExtension>()
            jacocoExtension?.apply {
                // The excludes list above should prevent instrumentation, but we also set
                // a system property as a fallback for tools that check it
                systemProperty("jacoco.excludes", "org.robolectric.*:android.*:androidx.*:com.android.*")
            }
        }
    }
    
    // Configure JaCoCo report task to exclude the same packages (if it exists)
    afterEvaluate {
        tasks.withType<JacocoReport> {
            classDirectories.setFrom(
                classDirectories.files.map {
                    project.fileTree(it) {
                        exclude(
                            "**/org/robolectric/**",
                            "**/android/**",
                            "**/androidx/**",
                            "**/com/android/**"
                        )
                    }
                }
            )
        }
    }
    
    sourceSets {
        getByName("test") {
            java.srcDirs("src/test/java")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Room database
    implementation("androidx.room:room-runtime:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")
    kapt("androidx.room:room-compiler:2.7.0")
    
    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    
    // JUnit 5 test engine and platform launcher for running tests
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.1")
}

// Configure JaCoCo test report task
tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    
    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/org/robolectric/**",
        "**/androidx/**",
        "**/com/android/**"
    )
    
    // Include both Java and Kotlin compiled classes
    val buildDir = layout.buildDirectory.get().asFile
    val javaClasses = fileTree("${buildDir}/intermediates/javac/debug") {
        exclude(fileFilter)
    }
    val kotlinClasses = fileTree("${buildDir}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }
    val mainSrc = "${project.projectDir}/src/main/java"
    
    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(javaClasses, kotlinClasses))
    executionData.setFrom(fileTree(buildDir) {
        include("jacoco/testDebugUnitTest.exec")
    })
}

