# ドキュメント一覧

[プロジェクトREADME](../README.md) · [用途別の検証ログ・証跡](logs/README.md)

構成や実行手順はこの階層の解説文書、実行ログ・結果JSON・画像などの証跡は`logs/`配下で管理します。

| 文書 | 内容 |
| --- | --- |
| [CLIENT_API_SPEC.md](CLIENT_API_SPEC.md) | CordovaPlugin・別アプリの2つのActivity起動経路、WebViewとKotlinの責務、Navigationの範囲、ゴルフの確定仕様・未確定事項。 |
| [VUE_WEBVIEW_INTEGRATION.md](VUE_WEBVIEW_INTEGRATION.md) | Vue Routerによる画面切り替え、既存Vueの共有、ビルド成果物のassets配布、2つのWebViewの通信アダプター。 |
| [WEB_TEAM_QUESTIONS.md](WEB_TEAM_QUESTIONS.md) | Vue組み込み検証前にWeb側へ共有する質問、業務APIまでの相談事項、他担当との確認事項。 |
| [WEBVIEW_BRIDGE_SAMPLE.md](WEBVIEW_BRIDGE_SAMPLE.md) | 実装済みの双方向通信サンプル、起動手順、通信契約、検証結果と残る制約。 |
| [WEBVIEW_BRIDGE_REVIEW.md](WEBVIEW_BRIDGE_REVIEW.md) | WebMessage双方向通信サンプル案のセキュリティ・ライフサイクル・既存構成との整合性レビュー。実装前の提案。 |
| [FEATURE_DEVELOPMENT.md](FEATURE_DEVELOPMENT.md) | Cordovaなしの開発APK、Android Studioでの実行、Debug限定のIntentサンプル。 |
| [CODE_OVERVIEW.md](CODE_OVERVIEW.md) | コード・設定の役割とファイル間の連携。 |
| [DEPENDENCY_VERSION_SELECTION.md](DEPENDENCY_VERSION_SELECTION.md) | 依存ライブラリ・Pluginの採用方針とバージョン選定理由。 |
| [DEPENDENCIES.md](DEPENDENCIES.md) | 通常依存の配置・共通bundleのapi公開・解決とコンパイルの確認。 |
| [NFC_READER_DESIGN.md](NFC_READER_DESIGN.md) | 汎用タグ検出、型番Profileからの通信方式・メモリ構成の解決、業務・共通FactoryとRepository実装の分離、ユースケース・画面との連携。 |
| [NFC_TAG_IDENTIFICATION.md](NFC_TAG_IDENTIFICATION.md) | 型番識別の共通Resolver・個別判定器・Hilt登録と、後から型番を追加する手順。 |
| [NFC_READING.md](NFC_READING.md) | ICODE SLI判定、ブロック・バイト座標による読取と業務ユースケース。 |
| [HILT.md](HILT.md) | 両ホストのApplication、Hilt ViewModel、KSPコード生成とJavaPoetのクラスパス。 |
| [LOGGING.md](LOGGING.md) | Timberの起動時初期化、Debug／Releaseの方針、ログの呼び出し・確認方法。 |
| [VERSION_CHECKS.md](VERSION_CHECKS.md) | Compose BOM・ktlint・detektの採用版と比較候補の検証。 |
| [VALIDATION.md](VALIDATION.md) | 初回のnative・Cordova統合とplatform再生成の検証。 |
| [ANDROID_CHECKS.md](ANDROID_CHECKS.md) | Gradle非推奨API・Android Lint・品質チェックの結果。 |
| [STATIC_ANALYSIS.md](STATIC_ANALYSIS.md) | 既存の静的解析・整形ルールを維持する方針と、追加前の相談事項。 |
| [GIT_HOOKS.md](GIT_HOOKS.md) | Git pre-commit・detektの仕様・導入・ケース別検証。 |
| [IMMUTABLE_COLLECTIONS.md](IMMUTABLE_COLLECTIONS.md) | 不変コレクションの導入と関連する静的解析ルール。 |
| [COMPOSE_RUNTIME.md](COMPOSE_RUNTIME.md) | プラグイン導入前のエミュレーターでのCompose表示・操作確認。 |
| [SERVICE_NAVIGATION.md](SERVICE_NAVIGATION.md) | 現在の暫定ネイティブ画面の起動・Navigation・引数復元と検証記録。 |
| [KOTLIN_PLUGIN.md](KOTLIN_PLUGIN.md) | Webからプラグイン経由でComposeを起動する構成・編集方法・確認結果。 |
| [KDOC.md](KDOC.md) | DokkaによるKDoc HTML生成・閲覧と表示確認。 |
| [UNIT_TESTS.md](UNIT_TESTS.md) | JUnit・Coroutines Test・JaCoCoの配置と実行方法。 |
