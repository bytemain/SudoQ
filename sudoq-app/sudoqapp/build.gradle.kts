plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

allprojects {
    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-Xlint:deprecation")
    }
}

android {
    namespace = "de.sudoq"
    testNamespace = "de.sudoq.test"
    compileSdk = 30
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    kotlin {
        jvmToolchain(17)
    }
    
    buildFeatures {
        compose = true
    }
    
    defaultConfig {
        applicationId = "de.sudoq"
        minSdk = 14
        targetSdk = 33
        
        resourceConfigurations.addAll(listOf("en", "de", "fr"))
        
        testApplicationId = "de.sudoq.test"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets {
        getByName("main") {
            // This allows us to group resources (layouts, values) by topic
            res.setSrcDirs(
                listOf(
                    "src/main/res/layouts/sudoku",
                    "src/main/res/layouts/tutorial",
                    "src/main/res/layout",
                    "src/main/res",
                    "src/main/res-screen/hints/",
                    "src/main/res-screen/main_menu/",
                    "src/main/res-screen/preferences/"
                )
            )
        }
    }

    buildTypes {
        release {
            // minifyEnabled = true
            // proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(project(":sudoqmodel"))
    implementation(libs.androidx.material)
    implementation(libs.androidx.appcompat)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Testing
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.kluent)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.junit.jupiter.engine)
}
