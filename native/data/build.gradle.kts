// 通信・永続化用Libraryの依存、単体テスト、KDoc HTMLの生成対象を定義する。
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
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
    implementation(libs.datastore.preferences)
    implementation(libs.datastore.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.datetime)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlin.result)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.ext.junit.ktx)
}
