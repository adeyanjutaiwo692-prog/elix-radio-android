plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.elixradio.app"
    compileSdk = 35

    buildFeatures { buildConfig = true }

    defaultConfig {
        applicationId = "com.elixradio.app"
        minSdk = 23
        targetSdk = 35
        buildConfigField("String", "SUPABASE_URL", "\"https://sumqnvpvtqpbbbytmbvk.supabase.co\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"sb_publishable_-H-6950SAms-43NMmbM0xQ_rTz-lGl0\"")
        versionCode = 4
        versionName = "1.2.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
