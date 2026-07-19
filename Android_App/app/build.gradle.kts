import java.io.FileInputStream
import java.util.Properties

// Load release signing credentials from a gitignored file (app/keystore.properties).
val keystoreProperties = Properties().apply {
    val propsFile = file("keystore.properties")
    if (propsFile.exists()) {
        load(FileInputStream(propsFile))
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.bhoomibot.os"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.bhoomibot.os"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            keystoreProperties.getProperty("STORE_FILE")?.let { storeFile = file(it) }
            keystoreProperties.getProperty("STORE_PASSWORD")?.let { storePassword = it }
            keystoreProperties.getProperty("KEY_ALIAS")?.let { keyAlias = it }
            keystoreProperties.getProperty("KEY_PASSWORD")?.let { keyPassword = it }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = false
            }
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
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    // Live internet link (robot<->operator over WebSocket).
    implementation(libs.okhttp)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation("androidx.compose.material:material-icons-extended")
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.orgjson)
    // Lets unit tests open a real local WebSocket endpoint to prove the live-link
    // client actually dials the relay (not just flips a UI flag). Matches libs.okhttp.
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    implementation("androidx.navigation:navigation-compose:2.9.3")
    implementation("androidx.camera:camera-camera2:1.6.1")
    implementation("androidx.camera:camera-lifecycle:1.6.1")
    implementation("androidx.camera:camera-view:1.6.1")
}