plugins {
    id("com.android.application")
    kotlin("android") version "2.0.21"
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
    }
}

dependencies {
    // Core Libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // CameraX dependencies
    implementation(libs.androidx.camera.core) // Core CameraX library
    implementation(libs.androidx.camera.lifecycle) // Lifecycle support for CameraX
    implementation(libs.androidx.camera.view) // CameraX Preview View
    implementation(libs.androidx.camera.camera2)
    implementation(libs.text.recognition)
    implementation (libs.androidx.navigation.fragment.ktx)
    implementation (libs.androidx.navigation.ui.ktx)
    // CameraX
    implementation(libs.androidx.camera.core.v130alpha04)
    implementation(libs.androidx.camera.camera2.v130alpha04)
    implementation(libs.androidx.camera.lifecycle.v130alpha04)
    implementation(libs.androidx.camera.view.v130alpha04)

    // Testing libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
