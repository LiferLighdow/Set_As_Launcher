plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.liferlighdow.sal"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.liferlighdow.sal"
        minSdk = 9
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    androidResources {
        localeFilters += listOf("en", "zh")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    packaging {
        resources {
            excludes += "/META-INF/**"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // No external dependencies for ultra-lightweight
}
