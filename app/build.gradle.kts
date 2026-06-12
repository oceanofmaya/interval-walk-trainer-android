import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("jacoco")
    id("dev.detekt")
}

android {
    namespace = "com.oceanofmaya.intervalwalktrainer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.oceanofmaya.intervalwalktrainer"
        minSdk = 24
        targetSdk = 36
        versionCode = 71
        versionName = "1.6.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    bundle {
        language {
            enableSplit = false
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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
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

base {
    archivesName.set("intervalwalktrainer-${android.defaultConfig.versionName}")
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0")
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("androidx.health.connect:connect-client:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // Room database
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    
    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.mockito:mockito-core:5.22.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.22.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.2.3")
    testImplementation("app.cash.turbine:turbine:1.2.1")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    
    // JUnit 6 test engine and platform launcher for running tests
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.3")
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

// detekt static analysis
detekt {
    config.setFrom(files("${rootProject.projectDir}/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    baseline.set(file("${rootProject.projectDir}/config/detekt/baseline.xml"))
    source.setFrom(
        "src/main/java",
        "src/test/java"
    )
}

