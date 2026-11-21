plugins {
    id("com.android.application")
}

android {
    namespace = "com.reprodutor.painel"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.reprodutor.painel"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation("org.nanohttpd:nanohttpd:2.3.1")
}