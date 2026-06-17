import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
}

android {
    namespace = "com.lakeshorestudios.nextwave"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lakeshorestudios.nextwave"
        minSdk = 26
        targetSdk = 35
        versionCode = 14
        versionName = "2.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Weather API key - override via local.properties or environment variable
        val weatherApiKey = project.findProperty("WEATHER_API_KEY") as? String
            ?: System.getenv("WEATHER_API_KEY")
            ?: "06de570cc7607ea17842332e0be7a605"
        buildConfigField("String", "WEATHER_API_KEY", "\"$weatherApiKey\"")
    }

    // Signing-Konfiguration aus local.properties
    val localProps = Properties().also { props: Properties ->
        val f = rootProject.file("local.properties")
        if (f.exists()) props.load(f.inputStream())
    }
    signingConfigs {
        create("release") {
            storeFile = file(localProps.getProperty("signing.storeFile", "${System.getProperty("user.home")}/keystores/nextwave-release.jks"))
            storePassword = localProps.getProperty("signing.storePassword", "")
            keyAlias = localProps.getProperty("signing.keyAlias", "nextwave")
            keyPassword = localProps.getProperty("signing.keyPassword", "")
        }
    }

    buildTypes {
        release {
            // Minify aktivieren, aber mit unseren verbesserten Proguard-Regeln
            isMinifyEnabled = true 
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        
        // Eine Debug-ähnliche Release-Version für Debugging-Zwecke
        create("debugRelease") {
            initWith(getByName("release"))
            isMinifyEnabled = false
            isShrinkResources = false
            matchingFallbacks.add("release")
            isDebuggable = true
            versionNameSuffix = "-debugRelease"
        }
        
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
        }
    }

    // Konfiguration zum Erstellen einer vollständigen APK anstelle eines App Bundles
    bundle {
        // Deaktiviert die Funktionen von App Bundle, sodass eine komplette APK erstellt wird
        abi {
            enableSplit = false
        }
        language {
            enableSplit = false
        }
        density {
            enableSplit = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Fix für common-path.so Problem
            pickFirst("lib/*/libandroidx.graphics.path.so")
        }
        // Nur native Bibliotheken einbeziehen, die tatsächlich verwendet werden
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

// Task zum Erstellen einer universellen APK (die nicht in App Bundles aufgeteilt wird)
tasks.register("createUniversalApk") {
    dependsOn("assembleRelease")
    doLast {
        println("✅ Universelle APK wurde erstellt!")
        println("📁 Die APK befindet sich unter: ${project.buildDir}/outputs/apk/release/app-release.apk")
        println("❗ Falls die Datei als 'unsigned' erscheint, müssen Sie die signing configuration in build.gradle.kts aktivieren.")
    }
}

// Task zum Erstellen einer Debug-APK für Release-Builds
tasks.register("createDebugReleaseApk") {
    dependsOn("assembleDebugRelease")
    doLast {
        println("✅ Debug-Release APK wurde erstellt!")
        println("📁 Die APK befindet sich unter: ${project.buildDir}/outputs/apk/debugRelease/app-debugRelease.apk")
        println("ℹ️ Diese Version behält Debug-Informationen bei, hat aber Release-Optimierungen aktiv.")
    }
}

// Update configuration to resolve Kotlin version conflict
configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlin.get()}")
        force("org.jetbrains.kotlin:kotlin-stdlib-common:${libs.versions.kotlin.get()}")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${libs.versions.kotlin.get()}")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${libs.versions.kotlin.get()}")
        force("org.jetbrains.kotlin:kotlin-reflect:${libs.versions.kotlin.get()}")
        
        // Exclude problematic Kotlin versions
        eachDependency {
            if (requested.group == "org.jetbrains.kotlin" && requested.version == "2.1.0") {
                useVersion(libs.versions.kotlin.get())
                because("Kotlin 2.1.0 is not compatible with the current toolchain")
            }
        }
    }
}

dependencies {
    // Force Kotlin standard library version
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlin.get()}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${libs.versions.kotlin.get()}")
    
    // Ensure Coroutines compatibility
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Core Android libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    
    // Splash Screen API
    implementation(libs.androidx.core.splashscreen)
    
    // Compose UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity.compose)
    
    // Material Icons
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    
    // Location
    implementation(libs.play.services.location)
    
    // Networking
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Supabase (Wave Check-in) - pinned to 2.x for Kotlin 1.9 compatibility
    implementation(platform("io.github.jan-tennert.supabase:bom:2.6.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.github.jan-tennert.supabase:gotrue-kt")
    implementation("io.ktor:ktor-client-android:2.3.12")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    
    // Image loading
    implementation(libs.coil)
    implementation(libs.coil.compose)
    
    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.navigation.compose)
    
    // Accompanist
    implementation(libs.accompanist.systemuicontroller)
    
    // Lucide Icons
    implementation(libs.lucide.icons)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
