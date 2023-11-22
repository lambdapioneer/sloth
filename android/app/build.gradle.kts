@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.com.android.application)
    alias(libs.plugins.org.jetbrains.kotlin.android)
}

android {
    namespace = "com.lambdapioneer.sloth.app"
    compileSdk = libs.versions.compilesdk.get().toInt()

    defaultConfig {
        applicationId = "com.lambdapioneer.sloth.app"
        minSdk = libs.versions.minsdk.get().toInt()
        compileSdk = libs.versions.compilesdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":sloth"))
    implementation(project(":sloth-pwhash-libsodium"))
    //implementation("com.lambdapioneer.sloth:sloth:0.1.1")
    //implementation("com.lambdapioneer.sloth:sloth-pwhash-libsodium:0.1.1")

    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.material3)
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}
