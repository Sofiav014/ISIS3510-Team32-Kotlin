plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    // Reemplaza kapt con ksp para versiones más recientes de Kotlin
    id("com.google.devtools.ksp") version "1.8.0-1.0.9"
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.example.sporthub"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.sporthub"
        minSdk = 26
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

    // Agregar soporte para viewBinding y dataBinding
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}

dependencies {
    // Firebase BoM

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation(platform("com.google.firebase:firebase-bom:32.3.1"))

    // Firebase Crashlytics and Analytics
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")

    // Otras dependencias de Firebase que necesitas
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Otras dependencias de AndroidX que podrías necesitar
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-ktx:1.8.0")
    implementation("androidx.fragment:fragment-ktx:1.6.1")

    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")

    // *** DEPENDENCIAS FALTANTES ***

    // Lifecycle components
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")

    // Google Sign-In (soluciona errores de GoogleSignIn)
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Glide para cargar imágenes (soluciona errores de Glide/bumptech)
    implementation("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")

    // Servicios de ubicación (soluciona errores de LocationServices)
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Para caching
    implementation("com.google.code.gson:gson:2.10.1")
}