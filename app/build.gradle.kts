plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.apkmerger"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.apkmerger"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources { excludes += setOf("META-INF/**", "**/*.kotlin_module") } }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("com.android.tools.build:apksig:8.5.2")
    implementation("androidx.activity:activity-ktx:1.9.3")
}
