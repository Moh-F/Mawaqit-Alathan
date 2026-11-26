import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun prop(name: String): String = providers.gradleProperty(name).get()

// Signing: from key.properties (local) or env vars (CI). Never commit the keystore.
val keyProps = Properties().apply {
    val f = rootProject.file("key.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signValue(name: String, env: String): String? = System.getenv(env) ?: keyProps.getProperty(name)

android {
    namespace = "com.mohdev.alathan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mohdev.alathan"
        minSdk = 24          // Fire OS 6 (Android 7.1) and up
        targetSdk = 35
        versionCode = (providers.gradleProperty("versionCode").orNull ?: prop("VERSION_CODE")).toInt()
        versionName = prop("VERSION_NAME")
        buildConfigField("String", "GITHUB_OWNER", "\"${prop("GITHUB_OWNER")}\"")
        buildConfigField("String", "GITHUB_REPO", "\"${prop("GITHUB_REPO")}\"")
    }

    val storePath = signValue("storeFile", "KEYSTORE_PATH")
    if (storePath != null) {
        signingConfigs {
            create("release") {
                storeFile = file(storePath)
                storeType = "pkcs12"
                storePassword = signValue("storePassword", "KEYSTORE_PASSWORD")
                keyAlias = signValue("keyAlias", "KEY_ALIAS")
                keyPassword = signValue("keyPassword", "KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    lint {
        abortOnError = false
        warningsAsErrors = false
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
