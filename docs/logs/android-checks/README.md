# Gradle・Android Lintの品質確認の記録

[ログ一覧へ戻る](../README.md) · [検証内容・実行方法](../../ANDROID_CHECKS.md)

保存済みの検証記録です。採用構成や実行手順の現状は、上の解説文書を参照してください。

| ファイル | 内容 |
| --- | --- |
| [android-gradle-check.log](android-gradle-check.log) | 非推奨API修正後の静的解析・整形確認・Android Lint。 |
| [android-lint-results.json](android-lint-results.json) | 修正後の各モジュール・variantのLint件数と内訳。 |
| [android-lint-error-gate.log](android-lint-error-gate.log) | UnusedResourcesを一時的にエラー化し、Lintがビルドを停止することを確認した失敗記録。 |
| [android-normal-build.log](android-normal-build.log) | 検証用ソースを除いた通常Debug／Release APKビルド。 |
| [compiler-options-probe.log](compiler-options-probe.log) | 型付きcompilerOptionsへの移行後のKotlin・Compose実コンパイル。 |
