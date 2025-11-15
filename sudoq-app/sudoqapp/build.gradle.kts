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
    compileSdk = 36
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    
    kotlin {
        jvmToolchain(21)
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            freeCompilerArgs.addAll(
                "-Xno-param-assertions",
                "-Xno-call-assertions",
                "-Xno-receiver-assertions"
            )
        }
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    defaultConfig {
        applicationId = "de.sudoq"
        minSdk = 21
        targetSdk = 36
        
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
        debug {
            // Use different package name for debug builds to allow installation alongside release version
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            // Debug version will display as "SudoQ DEBUG"
            resValue("string", "app_name", "SudoQ DEBUG")
        }
        
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
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Testing
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.kluent)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.junit.jupiter.engine)
}
