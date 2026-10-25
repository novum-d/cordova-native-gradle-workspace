# 依存関係の追加・解決・コンパイルの記録

[ログ一覧へ戻る](../README.md) · [検証内容・実行方法](../../DEPENDENCIES.md)

保存済みの検証記録です。採用構成や実行手順の現状は、上の解説文書を参照してください。

| ファイル | 内容 |
| --- | --- |
| [dependencies-build.log](dependencies-build.log) | 通常依存を追加した後のAPKビルド。 |
| [dependencies-catalog.log](dependencies-catalog.log) | Catalogライブラリ・PluginのArtifact解決。 |
| [dependencies-checks.log](dependencies-checks.log) | 通常依存追加後の静的解析・整形確認・Android Lint。 |
| [dependencies-cordova-compile.log](dependencies-cordova-compile.log) | Cordova側からの依存API・コード生成の実コンパイル。 |
| [dependencies-native-compile.log](dependencies-native-compile.log) | native側の依存API・コード生成の実コンパイル。 |
| [dependencies-lint-results.json](dependencies-lint-results.json) | 通常依存追加後のDebug／Release Lint件数。 |
| [dependencies-resolution-cordova.json](dependencies-resolution-cordova.json) | Cordova側の構成ごとの解決Artifact一覧。 |
| [dependencies-resolution-native.json](dependencies-resolution-native.json) | native側の構成ごとの解決Artifact一覧。 |
| [dependencies-resolution-native.log](dependencies-resolution-native.log) | native側のモジュール依存解決タスク実行。 |
| [resolved-catalog-libraries.txt](resolved-catalog-libraries.txt) | Catalogライブラリの解決座標と推移依存。 |
| [resolved-catalog-plugins.txt](resolved-catalog-plugins.txt) | Catalog Pluginの解決座標と推移依存。 |
| [resolved-host-runtime.txt](resolved-host-runtime.txt) | Cordovaホストの実行時依存の解決座標。 |
