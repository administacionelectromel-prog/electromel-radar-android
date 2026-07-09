plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.electromel.radar"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.electromel.radar"
        minSdk = 26          // Android 8+ — cubre los equipos de la zona
        targetSdk = 35
        versionCode = 1
        versionName = "7.0.0-alpha"
    }
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    // Room — persistencia local (equivalente nativo de IndexedDB)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    // Mapas OSM (equivalente nativo de Leaflet)
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    // Persistencia (equivalente de IndexedDB) — se agrega en la fase de datos
    // implementation("androidx.room:room-runtime:2.6.1")
    testImplementation("junit:junit:4.13.2")
}
