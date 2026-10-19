plugins {
    // Versions come from the native root OR Cordova's existing buildscript classpath.
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.example.nativefeature"
    buildToolsVersion = libs.versions.build.tools.get()
    compileSdk = libs.versions.compile.sdk.get().toInt()
    defaultConfig { minSdk = libs.versions.min.sdk.get().toInt() }
    // Only the catalog probe uses Compose; ordinary Activity sources do not.
    buildFeatures { compose = providers.gradleProperty("verifyCatalog").map { it == "true" }.getOrElse(false) }
    composeOptions { kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get() }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.target.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.target.get())
    }
    kotlinOptions { jvmTarget = libs.versions.jvm.target.get() }
}

dependencies {
    implementation(project(":data"))
    // Add catalog libraries when production sources actually use them.
}
