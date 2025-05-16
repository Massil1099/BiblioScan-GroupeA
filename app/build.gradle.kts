plugins {
    id("com.android.application")
    kotlin("android") version "2.0.21"
    id ("kotlin-parcelize")

}

android {
    namespace = "com.example.biblioscan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.biblioscan"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true

    }

    aaptOptions {
        noCompress("tflite")
    }
}

dependencies {
    // Core Libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // CameraX dependencies (1.3.0)
    implementation("androidx.camera:camera-core:1.3.0")
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")
    implementation("androidx.camera:camera-extensions:1.3.0")

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.3")

    // Text Recognition (OCR)
    implementation(libs.text.recognition)

    // Testing libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Networking (Ktor)
    implementation("io.ktor:ktor-client-core:2.3.4")
    implementation("io.ktor:ktor-client-cio:2.3.4")

    //Tensorflow Lite
    implementation ("org.tensorflow:tensorflow-lite:2.14.0")
    implementation ("org.tensorflow:tensorflow-lite-support:0.4.4")


    implementation ("com.google.mlkit:text-recognition:16.0.1")

}

