# ドキュメント一覧

[プロジェクトREADME](../README.md) · [用途別の検証ログ・証跡](logs/README.md)

構成や実行手順はこの階層の解説文書、実行ログ・結果JSON・画像などの証跡は`logs/`配下で管理します。

| 文書 | 内容 |
| --- | --- |
| [CODE_OVERVIEW.md](CODE_OVERVIEW.md) | コード・設定の役割とファイル間の連携。 |
| [DEPENDENCY_VERSION_SELECTION.md](DEPENDENCY_VERSION_SELECTION.md) | 依存ライブラリ・Pluginの採用方針とバージョン選定理由。 |
| [DEPENDENCIES.md](DEPENDENCIES.md) | 通常依存の配置・共通bundleのapi公開・解決とコンパイルの確認。 |
| [VERSION_CHECKS.md](VERSION_CHECKS.md) | Compose BOM・ktlint・detektの採用版と比較候補の検証。 |
| [VALIDATION.md](VALIDATION.md) | 初回のnative・Cordova統合とplatform再生成の検証。 |
| [ANDROID_CHECKS.md](ANDROID_CHECKS.md) | Gradle非推奨API・Android Lint・品質チェックの結果。 |
| [STATIC_ANALYSIS.md](STATIC_ANALYSIS.md) | 既存の静的解析・整形ルールを維持する方針と、追加前の相談事項。 |
| [GIT_HOOKS.md](GIT_HOOKS.md) | Git pre-commit・detektの仕様・導入・ケース別検証。 |
| [IMMUTABLE_COLLECTIONS.md](IMMUTABLE_COLLECTIONS.md) | 不変コレクションの導入と関連する静的解析ルール。 |
| [COMPOSE_RUNTIME.md](COMPOSE_RUNTIME.md) | プラグイン導入前のエミュレーターでのCompose表示・操作確認。 |
| [NFC_NAVIGATION.md](NFC_NAVIGATION.md) | Intentからの処理ID分岐、機能別Navigation、ViewModelのSavedStateHandleによる引数復元。 |
| [KOTLIN_PLUGIN.md](KOTLIN_PLUGIN.md) | Webからプラグイン経由でComposeを起動する構成・編集方法・確認結果。 |
| [KDOC.md](KDOC.md) | DokkaによるKDoc HTML生成・閲覧と表示確認。 |
| [UNIT_TESTS.md](UNIT_TESTS.md) | JUnit・Coroutines Test・JaCoCoの配置と実行方法。 |
