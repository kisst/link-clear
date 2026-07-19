plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// Single source of truth for the app version: the git tag (vMAJOR.MINOR.PATCH).
// versionName is the tag without the leading "v"; versionCode is derived as
// MAJOR*10000 + MINOR*100 + PATCH so it is monotonic across releases (required
// by F-Droid). Dev builds with no matching tag fall back to 0.0.0 / code 1 so
// the working tree always builds. F-Droid builds from the tag, so it always
// resolves a real version.
val gitVersionName: String =
    providers.exec {
        commandLine("git", "describe", "--tags", "--match", "v[0-9]*", "--abbrev=0")
        // Tolerate a repo with no matching tag (dev builds): don't fail the build.
        isIgnoreExitValue = true
    }.standardOutput.asText.map { it.trim().removePrefix("v") }
        .orElse("").get().ifBlank { "0.0.0" }

val gitVersionCode: Int =
    gitVersionName.split("-").first().split(".")
        .mapNotNull { it.toIntOrNull() }
        .let { parts ->
            val major = parts.getOrElse(0) { 0 }
            val minor = parts.getOrElse(1) { 0 }
            val patch = parts.getOrElse(2) { 0 }
            (major * 10_000 + minor * 100 + patch).coerceAtLeast(1)
        }

android {
    namespace = "app.linkclear"
    compileSdk = 35
    defaultConfig {
        applicationId = "app.linkclear"
        minSdk = 26
        targetSdk = 35
        versionCode = gitVersionCode
        versionName = gitVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    // Release signing is driven entirely by environment variables so no keystore
    // or secret ever lives in the repo (see docs/adr/0007-release-and-packaging.md).
    // CI populates these from GitHub Actions secrets; when unset (local builds,
    // lint/test CI) the release build is simply left unsigned.
    val keystorePath = System.getenv("LINKCLEAR_KEYSTORE")
    val hasSigning = !keystorePath.isNullOrBlank() && file(keystorePath).exists()
    if (hasSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = System.getenv("LINKCLEAR_STORE_PASSWORD")
                keyAlias = System.getenv("LINKCLEAR_KEY_ALIAS")
                keyPassword = System.getenv("LINKCLEAR_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}
dependencies {
    implementation(project(":core"))
    implementation(project(":unshorten"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime)
    implementation(libs.kotlinx.coroutines.android)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    debugImplementation(libs.compose.ui.test.manifest)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.okhttp.mockwebserver)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> { useJUnitPlatform() }
