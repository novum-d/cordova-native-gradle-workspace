// 共有Libraryのビルド基盤と、両モジュールをまとめるKDoc HTML生成を設定する。
plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.dokka)
}

// Cordovaと同じGradleを使い、Wrapperの更新元をCatalogへ統一する。
check(gradle.gradleVersion == libs.versions.gradle.get()) {
    "Use Gradle ${libs.versions.gradle.get()} from ./gradlew"
}

tasks.wrapper {
    gradleVersion = libs.versions.gradle.get()
    distributionType = Wrapper.DistributionType.BIN
}

// Dokka標準の集約機能で、feature・dataの文書を単一のHTMLサイトにする。
dependencies {
    dokka(project(":feature"))
    dokka(project(":data"))
}

dokka {
    moduleName.set("Cordova Native Libraries")
    dokkaPublications.html {
        includes.from("Module.md")
        suppressInheritedMembers.set(true)
    }
}

// 既存の依存構成検証とGit hookの品質チェックを、このホストにも登録する。
apply(from = "../verification/catalog-verification.gradle")

apply(from = "gradle/git-hooks.gradle.kts")
