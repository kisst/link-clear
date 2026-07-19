import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}
dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
tasks.test { useJUnitPlatform() }

// Target JVM 17 bytecode without requesting a provisioned toolchain: F-Droid's
// build server disables Gradle toolchain auto-provisioning, so jvmToolchain(17)
// fails there ("Cannot find a Java installation matching languageVersion=17").
// This compiles with whatever JDK 17+ is already running, matching the :app and
// :unshorten modules' sourceCompatibility/jvmTarget approach.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}
