plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":sdk"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.json:json:20240303")
    
    // Cloud AI Bridge
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
