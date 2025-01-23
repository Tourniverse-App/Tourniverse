plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services") // Firebase plugin
    id("androidx.navigation.safeargs.kotlin") // Safe Args plugin for navigation
}

android {
    namespace = "com.example.tourniverse"
    compileSdk = 35

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
    // Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.7.0")) // Firebase Bill of Materials

    // Firebase Dependencies
    implementation(libs.google.firebase.auth) // Authentication
    implementation("com.google.firebase:firebase-database") // Realtime Database
    implementation("com.google.firebase:firebase-analytics") // Analytics
    implementation("com.google.firebase:firebase-firestore-ktx") // Firestore
    implementation(libs.firebase.messaging) // Firebase Cloud Messaging

    // Google Play Services for Authentication
    implementation("com.google.android.gms:play-services-auth:21.3.0") // Google Play Services Auth

    // Material Components and AndroidX dependencies
    implementation("com.google.android.material:material:1.10.0") // Material Components
    implementation("androidx.recyclerview:recyclerview:1.2.1") // RecyclerView
    implementation("androidx.appcompat:appcompat:1.6.1") // AppCompat
    implementation("androidx.preference:preference:1.2.1") // Preference
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0") // CoordinatorLayout

    // Navigation Component
    implementation("androidx.navigation:navigation-runtime-ktx:2.8.4") // Navigation Runtime
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.4") // Navigation Fragment
    implementation("androidx.navigation:navigation-ui-ktx:2.8.4") // Navigation UI

    // Glide for Image Loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.androidx.junit.ktx)
    implementation(libs.androidx.espresso.idling.resource) // Glide
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0") // Glide Compiler

    // Test dependencies
    testImplementation("junit:junit:4.13.2") // JUnit
    androidTestImplementation("androidx.test.ext:junit:1.1.5") // AndroidX Test JUnit
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1") // Espresso Core
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.fragment:fragment-testing:1.5.7")
    testImplementation("androidx.arch.core:core-testing:2.1.0") // AndroidX Core Testing
    testImplementation("net.bytebuddy:byte-buddy:1.14.6")

    // For Kotlin test assertions
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.8.0") // Kotlin Test Assertions

    // For Mockito (mocking framework)
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.8.0") // Ensure JUnit compatibility
    testImplementation("org.robolectric:robolectric:4.10.3")

    // Core KTX
    implementation("androidx.core:core-ktx:1.12.0") // Core KTX

    // Firebase Dynamic Links
    implementation("com.google.firebase:firebase-dynamic-links-ktx:21.1.0") // Firebase Dynamic Links

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.7.1") // WorkManager
}
