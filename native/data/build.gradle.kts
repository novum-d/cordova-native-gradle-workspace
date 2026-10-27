// 通信・永続化用Libraryの依存、単体テスト、KDoc HTMLの生成対象を定義する。
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.serialization)
    // Hilt・KSPの実体は各ホストのルートでCatalogから読み込む。
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.dokka)
}

// Cordovaと同じAndroid・JVM基盤を使い、単体テストのカバレッジも計測する。
android {
    namespace = "dev.example.nativedata"
    buildToolsVersion = libs.versions.build.tools.get()
    compileSdk = libs.versions.compile.sdk.get().toInt()
    defaultConfig { minSdk = libs.versions.min.sdk.get().toInt() }
    testCoverage { jacocoVersion = libs.versions.jacoco.get() }
    buildTypes.configureEach { enableUnitTestCoverage = true }
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

// 画面への逆依存を作らず、通信・永続化・日時処理とテスト用の依存を設定する。
dependencies {
    // 共通依存を利用側にも公開し、dataに依存するモジュールから同じ型を使えるようにする。
    api(libs.bundles.native.common)
    implementation(libs.datastore.preferences)
    implementation(libs.datastore.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    // Hiltのコード生成は各モジュールで実行し、Compilerをapiへ公開しない。
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.ext.junit.ktx)
}
