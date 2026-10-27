// 既存featureのActivityへIntentサンプルを渡す、native単独の開発専用APKを定義する。
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Hilt・KSPの実体は各ホストのルートでCatalogから読み込む。
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

// Cordovaと共有する基盤版を使い、アプリIDだけを分けて同時インストールできるようにする。
android {
    namespace = "dev.example.nativefeature.dev"
    buildToolsVersion = libs.versions.build.tools.get()
    compileSdk = libs.versions.compile.sdk.get().toInt()
    defaultConfig {
        applicationId = "dev.example.nativefeature.dev"
        minSdk = libs.versions.min.sdk.get().toInt()
        targetSdk = libs.versions.target.sdk.get().toInt()
        versionCode = 1
        versionName = "1.0-dev"
    }
    // HiltのApplicationはLibraryではなく各APKで生成するため、共通ソースを直接コンパイルする。
    sourceSets.named("main") {
        java.srcDir("../host/src/main/kotlin")
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get() }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.target.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.target.get())
    }
}

// 開発用ランチャーとサンプルはsrc/debugだけに置き、Release variant自体を作らない。
androidComponents {
    beforeVariants(selector().withBuildType("release")) { variant -> variant.enable = false }
}

// KotlinとJavaの対象バイトコード版を共有Catalogへそろえる。
tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.jvmTarget.set(libs.versions.jvm.target.map(JvmTarget::fromTarget))
}

// 画面を複製せず既存featureへ依存し、ランチャーのCompose UIだけをこのモジュールに持つ。
dependencies {
    implementation(project(":feature"))
    // APK側でHiltのルートと全Libraryの注入先を集約する。
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
}
