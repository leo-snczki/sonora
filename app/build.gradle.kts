plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "dev.leosnczki.sonora"
    defaultConfig {
        applicationId = "dev.leosnczki.sonora"
        versionCode = 2
        versionName = "0.1.1"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    signingConfigs {
        // Release signing comes from the environment (CI secrets); without it the release build is unsigned.
        System.getenv("SONORA_KEYSTORE")?.let { keystore ->
            create("release") {
                storeFile = file(keystore)
                storePassword = System.getenv("SONORA_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SONORA_KEY_ALIAS")
                keyPassword = System.getenv("SONORA_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    androidResources {
        // English is the default; Portuguese (Brazil) is the only translation.
        localeFilters += listOf("en", "pt-rBR")
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(libs.androidx.exifinterface)
    implementation(project(":feature:backdrops"))
    implementation(project(":feature:home"))
    implementation(project(":wallpaper"))
    implementation(project(":core:media"))
    implementation(project(":core:data"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.robolectric)
}

android {
    testOptions.unitTests.all {
        it.systemProperty("sonora.rootDir", rootProject.projectDir.path)
    }
}
