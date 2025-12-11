import java.util.Properties
import java.io.FileInputStream

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.walksafe"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.walksafe"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Read keys from local.properties
        val recaptchaKey = localProperties.getProperty("recaptcha.site.key") ?: ""
        val webClientId = localProperties.getProperty("web.client.id") ?: ""
        val geminiApiKey = localProperties.getProperty("gemini.api.key") ?: ""
        val googleMapsKey = localProperties.getProperty("google.maps.key") ?: ""
        val azureApiKey = localProperties.getProperty("azure.api.key") ?: ""


        // Inject into BuildConfig (Kotlin)
        buildConfigField("String", "RECAPTCHA_KEY", "\"$recaptchaKey\"")
        buildConfigField("String", "WEB_CLIENT_ID", "\"$webClientId\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        // buildConfigField("String", "GROQ_API_KEY", "\"$groqApiKey\"")
        buildConfigField("String", "AZURE_API_KEY", "\"$azureApiKey\"")


        // Inject into Manifest (XML)
        manifestPlaceholders["MAPS_API_KEY"] = googleMapsKey
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation("com.vanniktech:android-image-cropper:4.6.0")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.3")

    // Firebase
    implementation("com.google.firebase:firebase-auth-ktx:22.3.1")
    implementation("com.google.firebase:firebase-firestore-ktx:24.10.2")
    implementation("com.google.firebase:firebase-storage-ktx:20.3.0")

    // Google Play Services
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.1.0")
    implementation("com.google.android.gms:play-services-recaptcha:17.0.1")

    // Google Maps Compose
    implementation("com.google.maps.android:maps-compose:4.3.0")

    // Gemini AI
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    // implementation("com.google.ai.client:generativeai:0.7.0")
    implementation("com.google.mlkit:image-labeling:17.0.7")

    // Image Loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    implementation("androidx.compose.material:material-icons-extended:1.6.3")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")

    // ML Kit (For Precise Privacy: Faces & Text)
    implementation("com.google.mlkit:face-detection:16.1.6")
    implementation("com.google.mlkit:object-detection:17.0.1")
    implementation("com.google.mlkit:text-recognition:16.0.0")
}