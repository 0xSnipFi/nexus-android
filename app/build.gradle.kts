import java.text.Normalizer
import java.util.Locale
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}
android {
    namespace = "app.nexus.discovery"
    compileSdk = 36
    defaultConfig {
        applicationId = "app.nexus.discovery"
        minSdk = 29
        targetSdk = 36
        versionCode = 12
        versionName = "0.4.8"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    lint { abortOnError = true }
}
dependencies {
    implementation(project(":domain"))
    implementation("org.jsoup:jsoup:1.18.3")
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.test.runner)
}










