plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    targetCompatibility = JavaVersion.VERSION_21
    sourceCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

dependencies {
    // Support for junit4 tests
    implementation(libs.junit) // TODO switch to junit5 entirely
    testImplementation(libs.junit.vintage.engine)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kluent)
    testImplementation(libs.mockk)

    testImplementation(libs.commons.io)
    testImplementation(libs.commons.lang3)
}

sourceSets {
    create("kotlintests") {
        kotlin.srcDir("$projectDir/src/test/kotlin")
        resources.srcDir("$projectDir/src/othertest/resources")
        compileClasspath += sourceSets["main"].output + sourceSets["test"].output
        runtimeClasspath += sourceSets["main"].output + sourceSets["test"].output
    }
    create("solverTests") {
        java.srcDir("$projectDir/src/solverTests/java")
        kotlin.srcDir("$projectDir/src/solverTests/kotlin")
        resources.srcDir("$projectDir/src/solverTests/resources")
        compileClasspath += sourceSets["main"].output + sourceSets["test"].output
        runtimeClasspath += sourceSets["main"].output + sourceSets["test"].output
    }
}

configurations {
    named("kotlintestsImplementation") {
        extendsFrom(configurations["testImplementation"])
    }
    named("kotlintestsRuntimeOnly") {
        extendsFrom(configurations["testRuntimeOnly"])
    }
    named("solverTestsImplementation") {
        extendsFrom(configurations["testImplementation"])
    }
    named("solverTestsRuntimeOnly") {
        extendsFrom(configurations["testRuntimeOnly"])
    }
}

tasks.register<Test>("kotlinTest") {
    testClassesDirs = sourceSets["kotlintests"].output.classesDirs
    classpath = sourceSets["kotlintests"].runtimeClasspath
}

tasks.register<Test>("solverTest") {
    testClassesDirs = sourceSets["solverTests"].output.classesDirs
    classpath = sourceSets["solverTests"].runtimeClasspath
}

tasks.named<ProcessResources>("processSolverTestsResources") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
