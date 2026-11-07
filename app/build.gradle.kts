plugins {
 alias(libs.plugins.android.application)
 alias(libs.plugins.kotlin.android)
 alias(libs.plugins.kotlin.compose)
}
android {
 namespace = "com.driveatlas"
 compileSdk = 35
 defaultConfig { applicationId = "com.driveatlas.alpha"; minSdk = 28; targetSdk = 35; versionCode = 1; versionName = "0.1.0-alpha" }
 buildTypes { debug { applicationIdSuffix = ".simulator"; buildConfigField("boolean", "SIMULATOR", "true") }; release { buildConfigField("boolean", "SIMULATOR", "false") } }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 buildFeatures { compose = true; buildConfig = true }
}
dependencies {
 implementation(libs.androidx.core.ktx)
 implementation(libs.androidx.lifecycle.runtime.ktx)
 implementation(libs.androidx.lifecycle.runtime.compose)
 implementation(libs.androidx.activity.compose)
 implementation(platform(libs.androidx.compose.bom))
 implementation(libs.androidx.ui)
 implementation(libs.androidx.material3)
 testImplementation(libs.junit)
 androidTestImplementation(libs.androidx.junit)
 androidTestImplementation(libs.androidx.espresso.core)
 androidTestImplementation(platform(libs.androidx.compose.bom))
 androidTestImplementation(libs.androidx.ui.test.junit4)
}
