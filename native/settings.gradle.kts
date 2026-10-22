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
    }
}

// このホストはAPKを持たず、featureとdataをLibraryとして読み込む。
rootProject.name = "CordovaNativeLibraries"
include(":feature", ":data")
// libsはgradle/libs.versions.tomlからGradleが自動登録するため、手動で二重登録しない。
