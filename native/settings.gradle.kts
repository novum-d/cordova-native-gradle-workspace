// native単独ビルドで使うPlugin・ライブラリの取得元と、共有Libraryの構成を定義する。
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// 取得元をSettingsで統一し、モジュールが独自のrepositoriesを追加したら失敗させる。
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 任意のCatalog検証がPluginのマーカーArtifactを依存として解決するときに使う。
        maven("https://plugins.gradle.org/m2/")
    }
}

// このホストはAPKを持たず、featureとdataをLibraryとして読み込む。
rootProject.name = "CordovaNativeLibraries"
include(":feature", ":data")
// libsはgradle/libs.versions.tomlからGradleが自動登録するため、手動で二重登録しない。
