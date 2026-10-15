plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.sms.sopnopay"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.sms.sopnopay"
        minSdk = 23
        targetSdk = 28
        versionCode = 2
        versionName = "1.1.0"
    }
}
dependencies {
    implementation("com.android.volley:volley:1.2.1")
}


kotlin {
    jvmToolchain(17)
}
