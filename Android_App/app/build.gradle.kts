plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.bhoomibot.os"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bhoomibot.os"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "1.0.8-REGRESSION"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnit4Runner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // 1. STANDALONE BRAIN LINK
    implementation("com.bhoomibot:ai-core")
    implementation("com.bhoomibot:ai-sdk")

    // 2. CORE ANDROID & COMPOSE
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    
    // 3. MATERIAL ICONS (CRITICAL FIX)
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    
    // 4. NAVIGATION & VIEWMODEL
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-service:2.7.0") // FIX FOR SERVICE CRASH
    
    // 5. DATA & NETWORKING
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.jwebsocket)
    
    // 6. GOOGLE MAPS
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    
    // 7. HARDWARE & COROUTINES
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    
    // 8. VISION & CAMERA
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("com.google.mediapipe:tasks-vision:0.10.14")
    implementation(libs.mediapipe.genai)
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.taskvision)
    
    // 9. QR & SCANNING
    implementation(libs.zxing.core)
    implementation(libs.mlkit.barcode)

    // 10. TESTING
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.uiautomator)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
