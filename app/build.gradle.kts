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
        versionCode = 6
        versionName = "3.0.2"

        // True only in the "staging" build type (see EmberConfig).
        buildConfigField("boolean", "STAGING", "false")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // Points at the staging site and Supabase project instead of
        // production. A separate app (".staging" ID, "Ember TV Staging") so it
        // installs next to the store app; debug-signed so Android Studio can
        // install it on a Fire TV. Never upload it to the Amazon Appstore.
        create("staging") {
            initWith(getByName("release"))
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("boolean", "STAGING", "true")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // --- Activity & Core ---
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")

    // --- COMPOSE BOM (CRITICAL: Version 2024.09.00 fixes key event issues) ---
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Material 3
    implementation("androidx.compose.material3:material3:1.3.0")

    // --- TV COMPOSE ---
    implementation("androidx.tv:tv-foundation:1.0.0-alpha11")
    implementation("androidx.tv:tv-material:1.0.0")

    // --- NETWORKING ---
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("io.coil-kt:coil-compose:2.7.0")

    // --- QR CODE (activation sign-in) ---
    implementation("com.google.zxing:core:3.5.3")

    // --- DEBUGGING ---
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // --- MEDIA3 (Version 1.5.0 fixes remote control lag) ---
    implementation("androidx.media3:media3-exoplayer:1.5.0")
    implementation("androidx.media3:media3-ui:1.5.0")
    implementation("androidx.media3:media3-common:1.5.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.5.0")
    implementation("androidx.media3:media3-datasource-okhttp:1.5.0")

    // --- OKHTTP ---
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}