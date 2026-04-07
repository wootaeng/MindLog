import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.ws.skelton.remind"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ws.skelton.remind"
        minSdk = 25
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProperties.getProperty("GEMINI_API_KEY")}\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "ADMOB_BANNER_ID", "\"${localProperties.getProperty("DEBUG_ADMOB_BANNER_ID")}\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${localProperties.getProperty("DEBUG_ADMOB_INTERSTITIAL_ID")}\"")
            buildConfigField("String", "ADMOB_NATIVE_ID", "\"${localProperties.getProperty("DEBUG_ADMOB_NATIVE_ID")}\"")
            buildConfigField("String", "ADMOB_OPENING_ID", "\"${localProperties.getProperty("DEBUG_ADMOB_OPENING_ID")}\"")
            manifestPlaceholders["ADMOB_APP_ID"] = localProperties.getProperty("DEBUG_ADMOB_APP_ID").toString()
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "ADMOB_BANNER_ID", "\"${localProperties.getProperty("RELEASE_ADMOB_BANNER_ID")}\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${localProperties.getProperty("RELEASE_ADMOB_INTERSTITIAL_ID")}\"")
            buildConfigField("String", "ADMOB_NATIVE_ID", "\"${localProperties.getProperty("RELEASE_ADMOB_NATIVE_ID")}\"")
            buildConfigField("String", "ADMOB_OPENING_ID", "\"${localProperties.getProperty("RELEASE_ADMOB_OPENING_ID")}\"")
            manifestPlaceholders["ADMOB_APP_ID"] = localProperties.getProperty("RELEASE_ADMOB_APP_ID").toString()
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
        compose = true
        buildConfig = true
    }
}

dependencies {
    // AI (Gemini)
    implementation(libs.google.generativeai)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    
    // Lifecycle & Activity
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.process)
    
    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose) // Navigation
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation("androidx.compose.ui:ui-text-google-fonts:1.7.0") // 추가

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.compose.material.core)
    ksp(libs.androidx.room.compiler)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp.logging.interceptor)

    // AdMob
    implementation("com.google.android.gms:play-services-ads:23.6.0")

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}