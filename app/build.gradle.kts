plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val updatePublicKeyFile = rootProject.file("update-signing-public.pem")
val updatePublicKey = updatePublicKeyFile.takeIf { it.exists() }?.readText()?.trim().orEmpty()
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\r", "\\r")
    .replace("\n", "\\n")

android {
    namespace = "com.buka.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.buka.mobile"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField(
            "String",
            "UPDATE_MANIFEST_URL",
            "\"https://gitee.com/hanhan2048/BukaMusic-Releases/raw/master/BukaMusic/latest.json\"",
        )
        buildConfigField(
            "String",
            "UPDATE_ALLOWED_HOSTS",
            "\"gitee.com,raw.giteeusercontent.com\"",
        )
        buildConfigField("String", "UPDATE_MANIFEST_PUBLIC_KEY", "\"$updatePublicKey\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.10.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-datasource:1.4.1")
    implementation("androidx.media:media:1.7.0")
    implementation(project(":usb-audio-driver"))
    // 1.0.6 raises minCompileSdk to 37. Keep the API-compatible 1.0.5
    // release so the client can continue targeting the installed API 36 SDK.
    implementation("io.github.kyant0:taglib:1.0.5")

    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
