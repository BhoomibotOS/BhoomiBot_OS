plugins {
    kotlin("jvm") version "2.2.10" apply false
    kotlin("plugin.serialization") version "2.2.10" apply false
}

allprojects {
    group = "com.bhoomibot"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        google()
    }
}
