# Git pre-commit・detektの動作確認の記録

[ログ一覧へ戻る](../README.md) · [検証内容・実行方法](../../GIT_HOOKS.md)

保存済みの検証記録です。採用構成や実行手順の現状は、上の解説文書を参照してください。

| ファイル | 内容 |
| --- | --- |
| [git-hooks-initial.log](git-hooks-initial.log) | ステージ済みKotlinがない初回preCommitのスキップ確認。 |
| [git-hooks-native.log](git-hooks-native.log) | native側の品質タスクとビルド。 |
| [git-hooks-cordova.log](git-hooks-cordova.log) | Cordova側の品質タスク。 |
| [git-hooks-cordova-build.log](git-hooks-cordova-build.log) | Git hook導入後のCordova APKビルド。 |
| [git-hooks-catalog.log](git-hooks-catalog.log) | Git hook用の開発依存追加後のCatalog解決。 |
| [git-hook-tests.log](git-hook-tests.log) | 初期9ケースの旧テスト結果。現在の30ケース集計とは別の履歴。 |
| [git-hook-browser-smoke.log](git-hook-browser-smoke.log) | 旧仕様のHTMLレポート表示確認。現在の仕様は解説文書を参照。 |

[ケース別の記録](cases/README.md)には、30ケースの結果集計と、保存されている個別ログ・実行後の状態JSONがあります。
