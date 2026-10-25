// Compose画面とその依存、単体テスト、KDoc HTMLの生成対象を定義する。
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    // AGP・Kotlinの版はnativeルート、またはCordovaが読み込んだbuildscriptの実体を使う。
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.dokka)
}

// Cordovaと同じAndroid・JVM基盤でComposeを有効にし、単体テストの計測も行う。
android {
    namespace = "dev.example.nativefeature"
    buildToolsVersion = libs.versions.build.tools.get()
    compileSdk = libs.versions.compile.sdk.get().toInt()
    defaultConfig { minSdk = libs.versions.min.sdk.get().toInt() }
    testCoverage { jacocoVersion = libs.versions.jacoco.get() }
    buildTypes.configureEach { enableUnitTestCoverage = true }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get() }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.target.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.target.get())
    }
}

// Kotlinのバイトコード対象をJava側と一致させる。
tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.jvmTarget.set(libs.versions.jvm.target.map(JvmTarget::fromTarget))
}

// Android共通のsource setだけで本体を文書化し、variant・テスト・生成ソースの重複を避ける。
dokka {
    dokkaPublications.html {
        includes.from("Module.md")
        suppressInheritedMembers.set(true)
    }
    dokkaSourceSets.configureEach {
        suppress.set(name != "main")
        documentedVisibilities.set(setOf(VisibilityModifier.Public, VisibilityModifier.Protected))
        sourceRoots.setFrom(layout.projectDirectory.dir("src/main/kotlin"))
        includes.from("Module.md")
        suppressGeneratedFiles.set(true)
        jdkVersion.set(17)
    }
}

// 画面からdataへ依存し、テスト・デバッグ専用の依存は対応する構成へ分ける。
dependencies {
    // 共通依存を利用側にも公開し、featureに依存するプラグインやappから同じ型を使えるようにする。
    api(libs.bundles.native.common)
    implementation(project(":data"))
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.foundation.layout)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.webkit)
    // Hiltのコード生成は各モジュールで実行し、Compilerをapiへ公開しない。
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.ext.junit.ktx)
}
