plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.nexauren.file"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nexauren.file"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    val keystorePath = System.getenv("NEXAUREN_KEYSTORE_PATH")
    if (!keystorePath.isNullOrBlank()) {
        signingConfigs.create("nexaurenRelease") {
            storeFile = file(keystorePath)
            storePassword = System.getenv("NEXAUREN_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("NEXAUREN_KEY_ALIAS")
            keyPassword = System.getenv("NEXAUREN_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (!keystorePath.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("nexaurenRelease")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
