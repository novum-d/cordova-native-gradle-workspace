// native単独ビルドで使うPlugin・ライブラリの取得元と、モジュール構成を定義する。
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

// 共有Library・プラグイン編集環境と、Cordovaなしで実行する開発専用APKを読み込む。
rootProject.name = "CordovaNativeLibraries"
include(":feature", ":data", ":cordova-plugin-dev", ":dev-app")
// libsはgradle/libs.versions.tomlからGradleが自動登録するため、手動で二重登録しない。
