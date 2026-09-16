plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.example.sealyoutube"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.sealyoutube"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("youtube.jks")
            storePassword = project.findProperty("RELEASE_STORE_PASSWORD")?.toString()
            keyAlias = project.findProperty("RELEASE_KEY_ALIAS")?.toString()
            keyPassword = project.findProperty("RELEASE_KEY_PASSWORD")?.toString()
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/baseline-prof.txt"
            excludes += "/*.dm"
            excludes += "**/baseline-prof.txt"
            excludes += "**/*.dm"
            excludes += "assets/dexopt/baseline.prof"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    configurations.all {
        exclude(group = "androidx.profileinstaller", module = "profileinstaller")
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.media)
    implementation(libs.okhttp)
    implementation(libs.json)
    implementation(libs.androidx.security.crypto)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
}
