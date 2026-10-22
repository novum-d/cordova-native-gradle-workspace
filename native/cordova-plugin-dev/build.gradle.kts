// プラグインの元ソースをAndroid Studioで編集・コンパイルするためのnative専用モジュール。
// Cordovaアプリはplugin.xmlから同じソースを導入するため、このモジュールには依存しない。
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.dokka)
}

android {
    namespace = "dev.example.kotlinplugin.development"
    buildToolsVersion = libs.versions.build.tools.get()
    compileSdk = libs.versions.compile.sdk.get().toInt()
    defaultConfig { minSdk = libs.versions.min.sdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.target.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.target.get())
    }
    // 複製せず元ファイルをsource setへ登録し、補完・参照解決・編集対象にする。
    sourceSets.named("main") {
        java.srcDir("../../kotlin-plugin/src/android")
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.jvmTarget.set(libs.versions.jvm.target.map(JvmTarget::fromTarget))
}

// プラグインのKDocも元ファイルから集約し、Cordova側の生成コピーは文書化しない。
dokka {
    dokkaPublications.html {
        includes.from("Module.md")
        suppressInheritedMembers.set(true)
    }
    dokkaSourceSets.configureEach {
        suppress.set(name != "main")
        documentedVisibilities.set(setOf(VisibilityModifier.Public, VisibilityModifier.Protected))
        sourceRoots.setFrom(layout.projectDirectory.dir("../../kotlin-plugin/src/android"))
        includes.from("Module.md")
        suppressGeneratedFiles.set(true)
        jdkVersion.set(17)
    }
}

// Cordova APIは編集・コンパイル時だけ必要。APKではホストのCordovaLibが実装を提供する。
dependencies {
    implementation(project(":feature"))
    compileOnly(libs.cordova.framework)
}
