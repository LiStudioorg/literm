/*
 * Copyright 2026 li63050a <li63050@foxmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.literm.listudioorg"
    compileSdk = 36

    // Release 签名：完全从环境变量读取，本地无 keystore 时自动回退为未签名构建。
    val releaseKeystore = file(System.getenv("KEYSTORE_FILE") ?: "release.keystore")
    val releaseSigningConfig = if (releaseKeystore.isFile) {
        signingConfigs.create("release") {
            storeFile = releaseKeystore
            storePassword = System.getenv("KEYSTORE_PASSWORD").orEmpty()
            keyAlias = System.getenv("KEY_ALIAS").orEmpty()
            keyPassword = System.getenv("KEY_PASSWORD").orEmpty()
            // 显式指定签名方案：V2 + V3（V1 对 minSdk 24 的包已无必要）。
            enableV1Signing = false
            enableV2Signing = true
            enableV3Signing = true
        }
    } else {
        null
    }

    defaultConfig {
        applicationId = "com.literm.listudioorg"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // 单一 Universal APK：不做 ABI 拆分，只在包内保留这两种 ABI 的 so。
        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
        }
    }

    // 单一 Universal APK：本项目从未启用 splits { abi { ... } }，因此不会按 ABI 拆分产物，
    // 只会输出一个 app-release.apk（内含上面 abiFilters 指定的两种 ABI）。

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = releaseSigningConfig ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // UpdateManager 需要 BuildConfig.VERSION_NAME 做版本比较。
        buildConfig = true
    }

    packaging {
        jniLibs {
            // libproot.so must be extracted to nativeLibraryDir so it can be executed.
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.10.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.activity:activity-compose:1.11.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("com.github.mwiede:jsch:2.28.8")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // 直接使用系统自带的 SQLite（SQLiteOpenHelper），不再依赖 Room。
    implementation("androidx.work:work-runtime-ktx:2.10.1")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
