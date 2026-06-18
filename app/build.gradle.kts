import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "ru.github.debitcredit"
//    compileSdk {
//        version = release(36) {
//            minorApiLevel = 1
//        }
//    }
    compileSdk = 36

    defaultConfig {
        applicationId = "ru.github.debitcredit"
        minSdk = 24
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }
}

// Добавляем параметры для Room KSP
ksp {
    arg("room.generateKotlin", "true")  // Генерировать Kotlin код вместо Java
    arg("room.incremental", "true")      // Инкрементальная компиляция
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // RecyclerView для списка категорий
    implementation(libs.androidx.recyclerview)

    // CardView для карточек категорий
    implementation(libs.androidx.cardview)

    // MPAndroidChart для графиков
    implementation(libs.mping.chart)

    // ViewModel и LiveData
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.7")

    // Room dependencies
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}