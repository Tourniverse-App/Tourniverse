plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.tourniverse"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.tourniverse"
        minSdk = 24
        targetSdk = 34
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
    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))

    // Add Firebase Analytics (if you want to use it)
    implementation("com.google.firebase:firebase-analytics")

    // Add Firebase Authentication (if you're using it)
    implementation("com.google.firebase:firebase-auth")

    // Add Firestore
    implementation("com.google.firebase:firebase-firestore")

    // Add Material Components library to resolve TextInputLayout
    implementation("com.google.android.material:material:1.6.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")

    // AndroidX Libraries
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui.ktx)

    // Test Libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
