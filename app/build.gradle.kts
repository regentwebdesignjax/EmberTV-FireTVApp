plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.regentmediagroup.embertv"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.regentmediagroup.embertv"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // --- CRITICAL MISSING DEPENDENCY ---
    implementation("androidx.activity:activity-compose:1.8.2")
    // -----------------------------------

    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Core Android & Compose
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Material 3 (Required for standard TextFields)
    implementation("androidx.compose.material3:material3:1.2.0")

    // TV Compose (Required for TV specific UI)
    implementation("androidx.tv:tv-foundation:1.0.0-alpha11")
    implementation("androidx.tv:tv-material:1.0.0-rc01")

    // Networking & Images
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Debugging
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // ExoPlayer (The Video Player)
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")
    implementation("androidx.media3:media3-common:1.2.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.2.0") // Required for HLS (.m3u8)

    // REQUIRED FOR THE FIX: Connects ExoPlayer to OkHttp
    implementation("androidx.media3:media3-datasource-okhttp:1.2.0")

    // Ensure standard OkHttp is available
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}