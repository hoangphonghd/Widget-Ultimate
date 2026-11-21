plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.magicos.widget"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.magicos.widget"
        minSdk = 26; targetSdk = 34
        versionCode = 4; versionName = "4.0"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
