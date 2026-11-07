plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "dev.leosnczki.sonora.wallpaper"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:render"))
    implementation(project(":core:media"))
    implementation(project(":core:data"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.robolectric)
}
