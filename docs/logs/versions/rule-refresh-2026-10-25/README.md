# 追加ルールの試行記録（2026-10-25・採用見送り）

[バージョン検証一覧](../README.md) · [現在のルール維持方針](../../../STATIC_ANALYSIS.md)

このフォルダは、追加ルールとCompose Rules 0.4.23への移行を試行した当時の記録です。以前のルールを尊重する方針により、追加設定・ルール依存の変更は取り消しました。現在の設定の検証結果としては扱いません。

native・Cordovaの実ソースと、共有品質タスクを複製した一時Gitリポジトリで確認しました。一時リポジトリは検証後に削除しています。

14項目すべて期待どおりです。違反例は終了コード1、正常例・自動整形は終了コード0を期待します。`results.json`の`sourceUnchanged`は読み取り専用処理で確認し、自動整形による変更は許容します。すべてのケースでGit indexは不変です。

| ファイル | 内容 |
| --- | --- |
| [native-check.log](native-check.log) | native側で更新後のdetekt・整形チェックを実行。 |
| [cordova-check.log](cordova-check.log) | Cordova側で同じルールを実行。 |
| [resolved-rules.log](resolved-rules.log) | detektPluginsの解決結果。Compose Rules 0.4.23と内蔵ktlintの版を確認。 |
| [results.json](results.json) | 一時リポジトリの14項目の集計。期待した違反検出・作業ソース・indexの保持を照合。 |
| [valid-source.log](valid-source.log) | 正常なソースの受理。 |
| [global-coroutine.log](global-coroutine.log) | GlobalScope.launchの拒否。 |
| [unused-private.log](unused-private.log) | 未使用private宣言の拒否。 |
| [cognitive-complexity.log](cognitive-complexity.log) | 深い条件分岐のCognitiveComplexMethod検出。 |
| [compose-remember.log](compose-remember.log) | rememberを伴わないState生成の拒否。 |
| [compose-modifier.log](compose-modifier.log) | modifierをルートに渡さず子だけに適用する誤りの拒否。 |
| [private-preview.log](private-preview.log) | IDEから使うprivate Previewを未使用扱いせず受理。 |
| [formatting-rejected.log](formatting-rejected.log) | 未使用import・不要なセミコロン・空白の違反を拒否。 |
| [formatting-autocorrect.log](formatting-autocorrect.log) | 違反を自動整形し、作業ソースを更新。 |
| [formatting-corrected.log](formatting-corrected.log) | 自動整形後の読み取り専用チェック成功。 |
| [formatting-idempotent.log](formatting-idempotent.log) | 整形済みソースへの再整形で内容が変わらないことを確認。 |
| [staged-formatting.log](staged-formatting.log) | 作業ソースが整形済みでも、index内の未整形ソースでpreCommitを拒否。 |
| [previous-formatting-comparison.log](previous-formatting-comparison.log) | 同じセミコロンを含む入力が旧設定では検査を通ることを比較。 |
| [invalid-config.log](invalid-config.log) | 未知の設定キーがある場合の設定検証エラー。 |
