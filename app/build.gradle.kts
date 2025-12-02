// Полное содержимое для ИСПРАВЛЕННОГО файла app/build.gradle.kts

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.hilt.android.gradle)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.qrscannerapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.qrscannerapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 24
        versionName = "1.3.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        shaders = true
    }

    composeOptions {
        // ВАЖНОЕ ИСПРАВЛЕНИЕ: Указание версии расширения компилятора Kotlin для AGP 8.x и API 36
        kotlinCompilerExtensionVersion = "1.5.11"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.05.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose Dependencies
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Hilt
    implementation(libs.hilt.android)
    // V-- ВОТ ЕДИНСТВЕННОЕ ИЗМЕНЕНИЕ: НЕПРАВИЛЬНЫЙ КОМПИЛЯТОР ЗАМЕНЕН НА ПРАВИЛЬНЫЙ --V
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")

    // Зависимости для WorkManager и его интеграции с Hilt
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Библиотека для графиков
    implementation("co.yml:ycharts:2.1.0")

    // Библиотека для диалогов с календарем
    implementation("io.github.vanpra.compose-material-dialogs:datetime:0.9.0")

    // V-- НАЧАЛО ИЗМЕНЕНИЙ: Проблемная библиотека удалена --V
    // implementation("com.valentinilk:shimmer:compose-shimmer:1.2.0")
    // ^-- КОНЕЦ ИЗМЕНЕНИЙ --^

    // Библиотека для работы с Excel (xlsx)
    implementation("org.apache.poi:poi-ooxml:5.2.5")

    // Тестовые зависимости
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Guava для решения конфликтов зависимостей
    implementation("com.google.guava:guava:32.1.3-android")

    // CameraX
    val camerax_version = "1.3.1"
    implementation("androidx.camera:camera-core:${camerax_version}")
    implementation("androidx.camera:camera-camera2:${camerax_version}")
    implementation("androidx.camera:camera-lifecycle:${camerax_version}")
    implementation("androidx.camera:camera-view:${camerax_version}")


// ML Kit
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("com.google.mlkit:text-recognition:16.0.0")

    // Jetpack Compose ViewModel & Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // DataStore для сохранения списка
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Для сериализации объектов в JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // Для загрузки шрифтов из Google Fonts
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.7")

    // SplashScreen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Для генерации QR-кодов
    implementation("com.google.zxing:core:3.5.1")

    // Зависимости для Room Database
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version")

    // --- НАЧАЛО ИЗМЕНЕНИЙ ---
    // Библиотека для загрузки изображений из сети
    implementation("io.coil-kt:coil-compose:2.6.0")
    // --- КОНЕЦ ИЗМЕНЕНИЙ ---

    // 3D Visualization (Sceneview)
    // V-- НАЧАЛО ИЗМЕНЕНИЙ --V
    // Обновляем библиотеку до версии, которая поддерживает нужные нам функции
    implementation("io.github.sceneview:sceneview:2.2.1")
    implementation("androidx.compose.material:material-icons-extended")
    // ^-- КОНЕЦ ИЗМЕНЕНИЙ --^
}