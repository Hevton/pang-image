plugins {
    alias(pangLibs.plugins.androidLibrary)
    alias(pangLibs.plugins.jetbrainsKotlinAndroid)
    alias(pangLibs.plugins.ktlint)
}

android {
    namespace = "io.lib.pang_image"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    ktlint {
        version.set("0.49.1") // 0.48 이상부터 editorconfig 기반만 사용
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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

    implementation(pangLibs.androidx.core.ktx)
    implementation(pangLibs.androidx.appcompat)
    implementation(pangLibs.material)
    testImplementation(pangLibs.junit)
    androidTestImplementation(pangLibs.androidx.junit)
    androidTestImplementation(pangLibs.androidx.espresso.core)
    implementation(pangLibs.retrofit)
}
