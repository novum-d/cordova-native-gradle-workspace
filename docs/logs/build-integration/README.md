# native・Cordovaのビルドと構成の記録

[ログ一覧へ戻る](../README.md) · [検証内容・実行方法](../../VALIDATION.md)

保存済みの検証記録です。採用構成や実行手順の現状は、上の解説文書を参照してください。

| ファイル | 内容 |
| --- | --- |
| [native-wrapper.log](native-wrapper.log) | Gradle 8.13のWrapper生成・設定読み込み。 |
| [native-tooling-sync.log](native-tooling-sync.log) | native側のAndroid V2モデル取得。 |
| [native-structure.log](native-structure.log) | feature・dataの通常AARビルド。 |
| [native-verification.log](native-verification.log) | 検証用ソースによるKotlin・Composeの実コンパイル。 |
| [native-final-check.log](native-final-check.log) | native側のJDK・Gradle・AGP・Kotlin・JVM targetと依存方向の照合。 |
| [cordova-tooling-sync.log](cordova-tooling-sync.log) | Cordova側のAndroid V2モデル取得。 |
| [cordova-structure.log](cordova-structure.log) | Cordova側のモジュール構成・APKホスト・基盤バージョンの照合。 |
| [cordova-probe-build.log](cordova-probe-build.log) | 検証用ソースを含むCordova APKのビルド。 |
| [cordova-probe-artifact-check.json](cordova-probe-artifact-check.json) | 検証APKのランチャーとfeature・dataの検証用assetsの照合。 |
| [cordova-build.log](cordova-build.log) | platform再生成後の通常Debug APKビルド。 |
| [cordova-release-and-structure.log](cordova-release-and-structure.log) | 通常Release APKビルドとCordova側の構成照合。 |
| [final-artifact-check.json](final-artifact-check.json) | 通常APKのSDK・ランチャー・検証コード除外・サイズ・SHA-256の照合。 |
| [apk-signature.log](apk-signature.log) | Debug APKの署名検証。 |
| [cordova-plugins.log](cordova-plugins.log) | プラグイン導入前のCordova Plugin一覧。 |
