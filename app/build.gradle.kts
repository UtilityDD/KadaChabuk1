plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // id("org.jetbrains.kotlin.plugin.serialization") version "1.9.21" // Keep if you use it for other things
    id("kotlin-kapt") // Required for Room's annotation processor with Kotlin
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

        // Optional: If you want to allow Room to export schema (good for version control)
        // javaCompileOptions {
        //     annotationProcessorOptions {
        //         arguments += mapOf(
        //             "room.schemaLocation" to "$projectDir/schemas".toString(),
        //             "room.incremental" to "true"
        //         )
        //     }
        // }
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
    // If you enable schema export, you might need to tell Android where to find them
    // sourceSets.getByName("androidTest") {
    //    assets.srcDirs("$projectDir/schemas")
    // }
}

dependencies {
    implementation("com.airbnb.android:lottie:5.2.0")
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.9.3") // Still needed for initial CSV parsing
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("com.google.android.material:material:1.12.0")
    // implementation("com.squareup.retrofit2:retrofit:2.9.0") // Keep if you use Retrofit for other network calls
    // implementation("com.squareup.retrofit2:converter-scalars:2.9.0") // Keep if needed for other calls
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    // implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0") // Keep if used elsewhere

    // Room components
    val roomVersion = "2.6.1" // Use the latest stable version
    implementation("androidx.room:room-runtime:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion") // Annotation processor for Kotlin
    implementation("androidx.room:room-ktx:$roomVersion") // Kotlin Extensions and Coroutines support for Room
    implementation("com.squareup.retrofit2:retrofit:2.9.0") // Or latest version
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0") //
    // Optional: For Room Paging 3 support (if you plan to use it later)
    // implementation("androidx.room:room-paging:$roomVersion")
}

