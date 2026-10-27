plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "online.malilab.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "online.malilab.app"
        minSdk = 24
        targetSdk = 35
        // Every GitHub build gets a higher number, so each new APK installs over the last one.
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "0.25.0"
    }

    signingConfigs {
        // A fixed test-signing key kept in the repository, so every build has the same signature
        // and updates install over the previous one. Replace with a private key before any store release.
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "malilab-debug"
            keyAlias = "malilabdebug"
            keyPassword = "malilab-debug"
        }
    }

    buildTypes {
        // Debug builds (the ones GitHub makes now) show Google's TEST ads, so nobody taps real ads while testing.
        // Release builds use MaliLab's real AdMob ids.
        debug {
            signingConfig = signingConfigs.getByName("debug")
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "BANNER_UNIT", "\"ca-app-pub-3940256099942544/9214589741\"")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            manifestPlaceholders["admobAppId"] = "ca-app-pub-4769800577067926~2189875520"
            buildConfigField("String", "BANNER_UNIT", "\"ca-app-pub-4769800577067926/9378722529\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-svg:2.7.0")
    implementation("com.google.android.gms:play-services-ads:23.5.0")
}
