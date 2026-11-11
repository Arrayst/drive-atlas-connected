plugins {
 alias(libs.plugins.android.application)
 alias(libs.plugins.kotlin.android)
 alias(libs.plugins.kotlin.compose)
}
android {
 namespace = "com.driveatlas"
 compileSdk = 35
 useLibrary("android.car")
 defaultConfig { applicationId = "com.driveatlas.connected"; minSdk = 28; targetSdk = 35; versionCode = 9; versionName = "0.9.0-roundtrip" }
 buildTypes { debug { applicationIdSuffix = ".emulator"; buildConfigField("boolean", "SIMULATOR", "true") }; release { buildConfigField("boolean", "SIMULATOR", "false") } }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 buildFeatures { compose = true; buildConfig = true }
}
dependencies {
 implementation("ai.pleos.playground:TextToSpeech:3.3.1")
 implementation("ai.pleos.playground:NaviHelper:3.0.5")
 implementation("ai.pleos.playground:FusedLocation:1.1.3")
 implementation(libs.androidx.core.ktx)
 implementation(libs.androidx.lifecycle.runtime.ktx)
 implementation(libs.androidx.lifecycle.runtime.compose)
 implementation(libs.androidx.activity.compose)
 implementation(platform(libs.androidx.compose.bom))
 implementation(libs.androidx.ui)
 implementation(libs.androidx.material3)
 testImplementation(libs.junit)
 testImplementation(libs.json)
 androidTestImplementation(libs.androidx.junit)
 androidTestImplementation(libs.androidx.espresso.core)
 androidTestImplementation(platform(libs.androidx.compose.bom))
 androidTestImplementation(libs.androidx.ui.test.junit4)
}
