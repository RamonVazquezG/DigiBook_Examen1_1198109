plugins {
    alias(libs.plugins.android.application)
}

android {
    // El namespace de tu repositorio
    namespace = "com.example.digibook_examen1_1198109"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.digibook_examen1_1198109"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        // Ajustado a Java 11 como en tu repositorio
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // Habilitar ViewBinding (sintaxis para .kts)
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Dependencias base (de tu repo)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // --- DEPENDENCIAS AÑADIDAS ---
    // Navigation Component
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    // Componentes de UI para el Dashboard
    implementation(libs.cardview)
    implementation(libs.gridlayout)

    // Dependencias de Test (de tu repo)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
