plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// The app version below is release-managed: it is written by ./release.sh, which
// then commits and tags, so the git tag stays the source of truth. The values
// are kept as inline literals (versionCode a bare integer, versionName a quoted
// string) because F-Droid's checkupdates parses this file statically with a
// regex and cannot run git or Gradle. Do not hand-edit; run ./release.sh <ver>.
// versionCode = MAJOR*10000 + MINOR*100 + PATCH, monotonic as F-Droid requires.
android {
    namespace = "app.linkclear"
    compileSdk = 35
    defaultConfig {
        applicationId = "app.linkclear"
        minSdk = 26
        targetSdk = 35
        versionCode = 10002
        versionName = "1.0.2"
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
