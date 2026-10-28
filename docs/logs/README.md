# 検証ログ・証跡一覧

[ドキュメント一覧へ戻る](../README.md)

ログ・結果JSON・依存一覧・UI XML・スクリーンショットを、確認した目的ごとにまとめています。各フォルダのREADMEに、ファイルごとの内容を記載しています。

これらは各文書に記載された検証時点の記録です。失敗ログには意図的な違反検出や互換性比較も含まれます。ログ内のコマンド・絶対パス・廃止済みタスク名は、当時の証跡として保持しています。

| フォルダ | 調べられる内容 | 解説・実行方法 |
| --- | --- | --- |
| [environment/](environment/README.md) | 実行環境 | [VALIDATION.md](../VALIDATION.md) |
| [build-integration/](build-integration/README.md) | native・Cordovaのビルドと構成 | [VALIDATION.md](../VALIDATION.md) |
| [regeneration/](regeneration/README.md) | Cordova platform再生成・設定復元hook | [VALIDATION.md](../VALIDATION.md) |
| [dependencies/](dependencies/README.md) | 依存関係の追加・解決・コンパイル | [DEPENDENCIES.md](../DEPENDENCIES.md) |
| [nfc-identification/](nfc-identification/README.md) | 型番判定器の登録・追加、技術別振分け、曖昧さと非同期識別の検証 | [NFC_TAG_IDENTIFICATION.md](../NFC_TAG_IDENTIFICATION.md) |
| [card-repository-factory/](card-repository-factory/README.md) | 業務・共通NFCのFactoryとRepository実装の分離、共通範囲読取・排他 | [NFC_READER_DESIGN.md](../NFC_READER_DESIGN.md) |
| [nfc-profile/](nfc-profile/README.md) | 型番Profileからの通信方式・メモリ構成の解決、範囲・応答検査と両ホストの確認 | [NFC_READER_DESIGN.md](../NFC_READER_DESIGN.md) |
| [nfc-detection/](nfc-detection/README.md) | 汎用タグ検出・業務Repository・同じカードの2項目取得と画面接続 | [NFC_READER_DESIGN.md](../NFC_READER_DESIGN.md) |
| [nfc-reading/](nfc-reading/README.md) | ICODE SLI分類・座標読取・ゴルフ場番号とホルダー番号の単体テスト、両ホストのビルド | [NFC_READING.md](../NFC_READING.md) |
| [hilt/](hilt/README.md) | 両ホストのHilt初期化・ViewModel生成と引数復元 | [HILT.md](../HILT.md) |
| [logging/](logging/README.md) | Timberの両ホスト初期化・Debug／Releaseの出力確認 | [LOGGING.md](../LOGGING.md) |
| [versions/](versions/README.md) | 採用版と更新候補の比較 | [VERSION_CHECKS.md](../VERSION_CHECKS.md) |
| [android-checks/](android-checks/README.md) | Gradle・Android Lintの品質確認 | [ANDROID_CHECKS.md](../ANDROID_CHECKS.md) |
| [git-hooks/](git-hooks/README.md) | Git pre-commit・detektの動作確認 | [GIT_HOOKS.md](../GIT_HOOKS.md) |
| [immutable-collections/](immutable-collections/README.md) | Immutable Collections導入の確認 | [IMMUTABLE_COLLECTIONS.md](../IMMUTABLE_COLLECTIONS.md) |
| [compose-runtime/](compose-runtime/README.md) | Compose画面のエミュレーター実行 | [COMPOSE_RUNTIME.md](../COMPOSE_RUNTIME.md) |
| [feature-development/](feature-development/README.md) | Cordovaなしの開発ホスト・Intentモック・Debug限定配置 | [FEATURE_DEVELOPMENT.md](../FEATURE_DEVELOPMENT.md) |
| [webview-bridge/](webview-bridge/README.md) | WebMessageの双方向通信・再接続・入力拒否とライフサイクル | [WEBVIEW_BRIDGE_SAMPLE.md](../WEBVIEW_BRIDGE_SAMPLE.md) |
| [intent-routing/](intent-routing/README.md) | サービスID分岐・サービス別Navigation・ViewModelの引数受け取りと復元 | [SERVICE_NAVIGATION.md](../SERVICE_NAVIGATION.md) |
| [nfc-navigation/](nfc-navigation/README.md) | 導入初期のホーム内サンプル遷移と引数の保存復元 | [SERVICE_NAVIGATION.md](../SERVICE_NAVIGATION.md) |
| [kotlin-plugin/](kotlin-plugin/README.md) | Kotlinプラグイン経由の画面起動 | [KOTLIN_PLUGIN.md](../KOTLIN_PLUGIN.md) |
| [kdoc/](kdoc/README.md) | KDocのHTML表示確認 | [KDOC.md](../KDOC.md) |

新しい検証記録も該当するフォルダへ置き、そのREADMEに目的を追記します。同じ検証で取得した画像やJSONはログと一緒に保管します。
