import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.daro.agentapp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.daro.agentapp"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["allowCleartextTraffic"] = "false"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "environment"
    productFlavors {
        create("staging") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", "\"http://43.224.34.135:3003/\"")
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
        }
        create("uat") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", "\"http://43.224.34.135:3003/\"")
            applicationIdSuffix = ".uat"
            versionNameSuffix = "-uat"
        }
        create("preProd") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", "\"http://43.224.34.135:3003/\"")
            applicationIdSuffix = ".preprod"
            versionNameSuffix = "-preProd"
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", "\"https://43.224.34.135:3003/\"")
        }
    }

    buildTypes {
        debug {
            // API 23 HTTP fallback; newer Android uses the debug network security XML.
            manifestPlaceholders["allowCleartextTraffic"] = "true"
            // Development server override, including prodDebug; never applies to release.
            buildConfigField("String", "API_BASE_URL", "\"http://43.224.34.135:3003/\"")
        }
        release {
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // Debug uses Android's automatically generated debug keystore.
    // Release credentials are optional for local debug builds and are never committed.
    val signingPropertiesFile = file("keys/key.properties")
    if (signingPropertiesFile.isFile) {
        val key = readProperties(signingPropertiesFile)
        val releaseSigning = signingConfigs.create("release") {
            keyPassword = key.getProperty("key_password")
            keyAlias = key.getProperty("alias")
            storePassword = key.getProperty("password")
            storeFile = file("keys/app.jks")
        }
        buildTypes.getByName("release").signingConfig = releaseSigning
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.mockwebserver)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
/**
 * Read gradle properties file
 */
fun readProperties(propertiesFile: File) = Properties().apply {
    propertiesFile.inputStream().use { fis ->
        load(fis)
    }
}
