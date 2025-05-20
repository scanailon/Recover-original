plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.unk.recoverrework"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.unk.recoverrework"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "2.0.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("libs")  // Para las librerias JNI
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += listOf(
                "/META-INF/spring.tooling",
                "/META-INF/spring.handlers",
                "/META-INF/spring.factories",
                "/META-INF/AL2.0",
                "/META-INF/license.txt",
                "/META-INF/LGPL2.1",
                "/META-INF/DEPENDENCIES",
                "/META-INF/spring.schemas",
                "/META-INF/notice.txt",
                "google/protobuf/*.proto",
                "license/README.dom.txt",
                "license/LICENSE.dom-documentation.txt",
                "license/NOTICE",
                "license/LICENSE.dom-software.txt",
                "license/LICENSE",
                "org/apache/commons/codec/language/*.txt",
                "org/apache/commons/codec/language/bm/*.txt"
            )
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Requerido para que la app funcione
    implementation(libs.kongzue.dialogx) // DialogX
    implementation(libs.baserecyclerviewadapterhelper4) // BRVAH
    implementation(libs.androidx.recyclerview) // RecyclerView
    implementation(fileTree("libs")) // Contenidos de la carpeta "libs", que se encuentra en la carpeta "app"
    implementation(libs.androidx.swiperefreshlayout) // SwipeRefreshLayout
    implementation(libs.ycharts) // YCharts
    implementation(libs.poi) // POI
    implementation(libs.rest.client) // ThingsBoard
}

configurations {
    all {
        exclude(group = "commons-io", module = "commons-io")
        exclude(group = "org.apache.commons.codec.language.bm")
        exclude(group = "org.slf4j", module = "jcl-over-slf4j")
    }
}
