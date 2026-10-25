# Git hookのケース別検証記録

[Git hookの記録へ戻る](../README.md) · [仕様・検証結果](../../../GIT_HOOKS.md)

[results.json](results.json)が30ケースの集計です。`passed`は期待どおりだったかを示し、違反検出などのケースでは終了コード1を期待します。

`<ケース名>.log`は実行ログ、`<ケース名>-after.json`は実行後の状態記録です。集計だけが残っているケースもあり、個別記録があるものだけリンクを掲載しています。

| ケース | 確認内容 | 保存された記録 |
| --- | --- | --- |
| `no-staged-kotlin` | ステージ済みKotlinがなければスキップ | [実行ログ](no-staged-kotlin.log)、[実行後の状態](no-staged-kotlin-after.json) |
| `clean-staged` | 整形済みKotlinのコミットを許可 | [実行ログ](clean-staged.log)、[実行後の状態](clean-staged-after.json) |
| `formatting-changed` | 自動整形で変更が生じたら停止 | [実行ログ](formatting-changed.log)、[実行後の状態](formatting-changed-after.json) |
| `retry-without-restaging` | 再ステージしない再試行を拒否 | [実行ログ](retry-without-restaging.log)、[実行後の状態](retry-without-restaging-after.json) |
| `retry-after-restaging` | 整形後の再ステージで成功 | [実行ログ](retry-after-restaging.log)、[実行後の状態](retry-after-restaging-after.json) |
| `existing-unstaged` | 既存の未ステージ差分を保持 | [実行ログ](existing-unstaged.log)、[実行後の状態](existing-unstaged-after.json) |
| `analysis-failed` | 静的解析違反で停止 | [実行ログ](analysis-failed.log)、[実行後の状態](analysis-failed-after.json) |
| `browser-disabled` | 設定によるブラウザー表示の無効化 | [実行ログ](browser-disabled.log)、[実行後の状態](browser-disabled-after.json) |
| `ci-skips-browser` | CIではブラウザー表示を省略 | [実行ログ](ci-skips-browser.log)、[実行後の状態](ci-skips-browser-after.json) |
| `browser-failure-preserves-analysis-failure` | ブラウザー起動失敗時も解析違反による停止を維持 | [実行ログ](browser-failure-preserves-analysis-failure.log)、[実行後の状態](browser-failure-preserves-analysis-failure-after.json) |
| `browser-fallback-succeeds` | 既定ブラウザー失敗後のフォールバック | [実行ログ](browser-fallback-succeeds.log)、[実行後の状態](browser-fallback-succeeds-after.json) |
| `analysis-execution-failed` | 解析実行エラー時に古いHTMLを開かない | [集計のみ](results.json) |
| `invalid-detekt-config` | 不正なdetekt設定で停止 | [集計のみ](results.json) |
| `remaining-formatting-violation` | 自動整形で補正できない違反で停止 | [集計のみ](results.json) |
| `deletion-only` | Kotlinの削除だけならスキップ | [実行ログ](deletion-only.log)、[実行後の状態](deletion-only-after.json) |
| `spaces-and-unicode` | 空白・日本語を含むファイル名を処理 | [実行ログ](spaces-and-unicode.log)、[実行後の状態](spaces-and-unicode-after.json) |
| `unstaged-formatting-changed` | 未ステージの別ファイルの整形変更も検出 | [実行ログ](unstaged-formatting-changed.log)、[実行後の状態](unstaged-formatting-changed-after.json) |
| `check-does-not-format` | 読み取り専用チェックは整形しない | [実行ログ](check-does-not-format.log)、[実行後の状態](check-does-not-format-after.json) |
| `staged-formatting-only` | 作業ファイルが整形済みでもステージ内の違反を検出 | [実行ログ](staged-formatting-only.log)、[実行後の状態](staged-formatting-only-after.json) |
| `staged-formatting-without-working-file` | 作業ファイル消失時もステージ内容を検査 | [実行ログ](staged-formatting-without-working-file.log)、[実行後の状態](staged-formatting-without-working-file-after.json) |
| `staged-formatting-spaces-and-unicode` | 空白・日本語を含むステージ済みファイルの違反を検出 | [実行ログ](staged-formatting-spaces-and-unicode.log)、[実行後の状態](staged-formatting-spaces-and-unicode-after.json) |
| `alternate-index-formatting-only` | GIT_INDEX_FILEで指定されたindexを検査 | [実行ログ](alternate-index-formatting-only.log)、[実行後の状態](alternate-index-formatting-only-after.json) |
| `staged-formatting-html-name` | HTML特殊文字を含むファイル名の違反を検出 | [実行ログ](staged-formatting-html-name.log)、[実行後の状態](staged-formatting-html-name-after.json) |
| `data-class-mutable` | data classのvarを検出 | [実行ログ](data-class-mutable.log)、[実行後の状態](data-class-mutable-after.json) |
| `data-class-immutable` | data classのvalを許可 | [実行ログ](data-class-immutable.log)、[実行後の状態](data-class-immutable-after.json) |
| `compose-list` | ComposableのList引数を検出 | [実行ログ](compose-list.log)、[実行後の状態](compose-list-after.json) |
| `compose-set` | ComposableのSet引数を検出 | [実行ログ](compose-set.log)、[実行後の状態](compose-set-after.json) |
| `compose-map` | ComposableのMap引数を検出 | [実行ログ](compose-map.log)、[実行後の状態](compose-map-after.json) |
| `compose-mutable-list` | ComposableのMutableList引数を検出 | [実行ログ](compose-mutable-list.log)、[実行後の状態](compose-mutable-list-after.json) |
| `compose-immutable-collections` | Composableの不変コレクション引数を許可 | [実行ログ](compose-immutable-collections.log)、[実行後の状態](compose-immutable-collections-after.json) |
| `browser-command-fails` | ブラウザー起動コマンドの失敗 | [実行ログ](browser-command-fails.log)、[実行後の状態](browser-command-fails-after.json) |
