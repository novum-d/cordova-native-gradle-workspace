// Library・開発ホストのビルド基盤と、Library・プラグインの集約KDocを設定する。
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    // AGPと同じ親クラスパスで解決し、Hiltが必要なJavaPoetも正しい版へそろえる。
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
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

// Dokka標準の集約機能で、Libraryとプラグインの文書を単一のHTMLサイトにする。
dependencies {
    dokka(project(":feature"))
    dokka(project(":data"))
    dokka(project(":cordova-plugin-dev"))
}

dokka {
    moduleName.set("Cordova Native Libraries")
    dokkaPublications.html {
        includes.from("Module.md")
        suppressInheritedMembers.set(true)
    }
}

// Git hookと通常checkの品質タスクを、このホストにも登録する。
apply(from = "gradle/git-hooks.gradle.kts")
