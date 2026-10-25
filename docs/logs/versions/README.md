# 採用版と更新候補の比較の記録

[ログ一覧へ戻る](../README.md) · [検証内容・実行方法](../../VERSION_CHECKS.md)

保存済みの検証記録です。採用構成や実行手順の現状は、上の解説文書を参照してください。

| ファイル | 内容 |
| --- | --- |
| [compose-bom-latest-probe.log](compose-bom-latest-probe.log) | Compose BOM 2026.09.00の比較ビルド。Kotlinメタデータ非互換による失敗記録。 |
| [compose-bom-selected-probe.log](compose-bom-selected-probe.log) | 採用BOM 2025.07.00での比較ビルド成功記録。 |
| [quality-detekt-latest.log](quality-detekt-latest.log) | detekt 1.23.8の解析・整形チェック。 |
| [quality-ktlint-latest.log](quality-ktlint-latest.log) | 独立した一時プロジェクトでのktlint比較。意図的な違反検出と整形後の成功を含む。 |

[2026-10-25の追加ルール試行（採用見送り）](rule-refresh-2026-10-25/README.md)には、試行時の実行ログと一時リポジトリでの検証結果を保存しています。試行した追加設定は現在適用していません。
