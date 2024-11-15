plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.kmou.cslogin"
    compileSdk = 35
    viewBinding {
        enable = true
    }

    defaultConfig {
        applicationId = "com.kmou.cslogin"
        minSdk = 30
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.health:health-services-client:1.1.0-alpha03")
    implementation ("androidx.health.connect:connect-client:1.1.0-alpha10")
    implementation ("androidx.activity:activity-ktx:1.9.2")  // AndroidX Activity
    implementation("com.android.volley:volley:1.2.1")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.4.0")

    // Firebase BoM (Bill of Materials) for managing versions
    implementation(platform("com.google.firebase:firebase-bom:32.1.0"))

    // Firebase Authentication and Analytics (version handled by BoM)
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-firestore")
    // Google Play services library for authentication
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Updated Facebook SDK for AndroidX compatibility
    implementation("com.facebook.android:facebook-login:latest.release")

    implementation ("com.kakao.sdk:v2-all:2.20.6") // 전체 모듈 설치, 2.11.0 버전부터 지원

    // AndroidX dependencies
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.core.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}