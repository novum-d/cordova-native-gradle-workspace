// Compose画面とその依存、単体テスト、KDoc HTMLの生成対象を定義する。
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    // AGP・Kotlinの版はnativeルート、またはCordovaが読み込んだbuildscriptの実体を使う。
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    // 型付き画面引数のserializerを生成し、Navigationの保存・復元で利用する。
    alias(libs.plugins.kotlin.serialization)
    // Hilt・KSPの実体は各ホストのルートでCatalogから読み込む。
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
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
    // ホスト共通の起動時初期化をManifestから登録し、TimberのTreeを一度だけ用意する。
    implementation(libs.androidx.startup.runtime)
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
    // 画面ごとのViewModelはSavedStateHandleからNavigationの引数を復元する。
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    // サービスごとの画面に渡す型付きルートと、任意のJSONオブジェクト引数を扱う。
    implementation(libs.androidx.navigation.compose)
    // Navigationの各エントリを所有者として、SavedStateHandle付きのHilt ViewModelを生成する。
    implementation(libs.hilt.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.webkit)
    // Hiltのコード生成は各モジュールで実行し、Compilerをapiへ公開しない。
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.ext.junit.ktx)
}
