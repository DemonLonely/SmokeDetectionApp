plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.smokedetection"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.smokedetection"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        // để dùng `var` ok
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Material UI
    implementation("com.google.android.material:material:1.12.0")

    // RecyclerView nếu project bạn chưa có
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // CameraX (Live Cam)
    val cameraxVersion = "1.4.0"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Glide (load image)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // (khuyến nghị) Material UI
    implementation("com.google.android.material:material:1.12.0")
}
