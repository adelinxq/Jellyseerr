plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.jellyseerr.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.jellyseerr.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"

        // Pentru sistemul de update
        buildConfigField("int", "VERSION_CODE", "$versionCode")
        buildConfigField("String", "VERSION_NAME", "\"$versionName\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    buildFeatures {
        buildConfig = true
        viewBinding = true
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // ========== ANDROIDX ==========
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // ========== MATERIAL DESIGN ==========
    implementation("com.google.android.material:material:1.11.0")

    // ========== WEBVIEW ==========
    implementation("androidx.webkit:webkit:1.9.0")

    // ========== COROUTINES ==========
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ========== FIREBASE (OPTIONAL) ==========
    // Dacă folosești Firebase, de-comentează:
     implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
     implementation("com.google.firebase:firebase-messaging-ktx")
     implementation("com.google.firebase:firebase-analytics-ktx")

    // ========== TESTING ==========
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // ========== BIOMETRIC AUTH (DACĂ FOLOSEȘTI) ==========
    // Dacă ai nevoie de autentificare biometrică, de-comentează:
    // implementation("androidx.biometric:biometric:1.1.0")

    // ========== SECURITY (DACĂ FOLOSEȘTI) ==========
    // Dacă ai nevoie de criptare, de-comentează:
    // implementation("androidx.security:security-crypto:1.1.0-alpha03")
}