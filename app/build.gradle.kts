import java.io.FileInputStream
import java.util.Properties;

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    alias(libs.plugins.kotlin.android)
}

// API 키 로드
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

// API 키 로드
val androidClientId: String? = localProperties.getProperty("ANDROID_CLIENT_ID")
val kakaoAppKey: String? = localProperties.getProperty("KAKAO_APP_KEY")
val apiKey: String? = localProperties.getProperty("API_KEY")

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

        // BuildConfig에 API 키 추가
        buildConfigField("String", "ANDROID_CLIENT_ID", "\"$androidClientId\"")
        buildConfigField("String", "KAKAO_APP_KEY", "\"$kakaoAppKey\"")
        buildConfigField("String", "API_KEY", "\"$apiKey\"")

        // strings.xml에서 사용할 수 있도록 추가
        resValue("string", "android_client_id", androidClientId ?: "")
        resValue("string", "kakao_app_key", kakaoAppKey ?: "")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        resValues = true // Gradle 변수로 strings.xml 값을 설정 가능하게 함
    }
}

dependencies {
    // Health Services
    implementation("androidx.health:health-services-client:1.1.0-alpha03")
    implementation("androidx.health.connect:connect-client:1.1.0-alpha10")

    // AndroidX 및 기본 라이브러리
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.4.0")
    implementation("androidx.annotation:annotation:1.6.0")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.core.ktx)

    // 네트워킹 라이브러리
    implementation("com.android.volley:volley:1.2.1")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    // Firebase SDK
    implementation(platform("com.google.firebase:firebase-bom:32.1.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-firestore")

    // Google Play Services (로그인 등)
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Kakao SDK
    implementation("com.kakao.sdk:v2-all:2.20.6") // Kakao 전체 모듈 설치

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}