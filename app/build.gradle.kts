plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.21" // Or your Kotlin version
}
android {
    namespace = "com.blackgrapes.kadachabuk"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.blackgrapes.kadachabuk"
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
}

dependencies {
    implementation ("com.airbnb.android:lottie:5.2.0")
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.9.3") // Or latest version
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.glance:glance-appwidget:1.1.0") // Or the latest version
    implementation("com.google.android.material:material:1.12.0") // For Material Design components like CardView
    implementation("com.squareup.retrofit2:retrofit:2.9.0") // For network requests
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0") // For handling plain text/CSV response
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0") // For ViewModel
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0") // For LiveData
    implementation("androidx.recyclerview:recyclerview:1.3.2") // For displaying lists
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0") // Or latest version
}