# feature単独開発とIntentサンプルの検証

[ログ一覧](../README.md) · [開発・実行手順](../../FEATURE_DEVELOPMENT.md)

確認日: 2026-10-25。API 35 / arm64のCordova_Compose_API_35で、開発専用のdev-appと既存Cordovaアプリを同時にインストールして確認しました。実機固有の挙動は未確認です。

| ファイル | 内容 |
| --- | --- |
| [native-build-and-tasks.log](native-build-and-tasks.log) | dev-appのDebug APK・Lint・タスク一覧。Release variantが作られないことも確認 |
| [install.log](install.log) | npm run dev:installによるビルド・実際のインストール |
| [android-checks.log](android-checks.log) | native・Cordovaの静的解析・整形・単体テスト・Lint。開発ホストも対象 |
| [cordova-build.log](cordova-build.log) | 開発ホスト追加後の既存Cordova Debug APKビルド |
| [kdoc.log](kdoc.log) | 既存Library・PluginのKDoc生成 |
| [runtime-results.json](runtime-results.json) | 画面選択・型付き引数・不正要求・onNewIntent・回転・プロセス再生成・標準WebViewの実行結果 |
| [packaging-results.json](packaging-results.json) | 両APKのSHA-256、開発ランチャーとサンプルの有無、feature Activityの非公開設定 |
| [lint-summary.json](lint-summary.json) | dev-appのLint警告と、既存feature・dataのvariant別テスト件数 |
| [launcher.png](launcher.png) | 最終Debug APKのサンプル選択画面 |
| [launcher-layout.json](launcher-layout.json) | 最終ランチャーのUI階層 |
| [golf-structured-layout.json](golf-structured-layout.json) | 日本語・記号・入れ子のargsを受け取った既存ゴルフ画面 |
| [process-restored-layout.json](process-restored-layout.json) | プロセス再生成後も最新のゴルフ画面・引数が復元されたUI |

## 確認した動作

- UIから、空args・省略・入れ子の3つのゴルフ要求とhomeを起動。
- 公開されたDebugランチャーにadbでsampleを指定し、同じfeatureのActivityRecordを保ってonNewIntentで要求を切り替え。
- 未対応IDと配列argsはfeatureの本物の受信処理で拒否され、プロセスをクラッシュさせずランチャーへ戻る。
- 未知のサンプル名はランチャー内で拒否。外部入力を任意のassetsパスとして扱わない。
- featureの回転で引数を復元。ランチャー自身の回転では、以前のsample Extraを再実行しない。
- バックグラウンドへの遷移後にプロセスをam killし、別PIDで最新の画面・引数が復元されることを確認。
- Cordovaを経由せず標準WebViewでREADY・公開APIの実fetch・Composeへの表示・疑似NFCのCustomEventを確認。
- 開発APKには要求JSONが7ファイル、DevLauncherActivityとIntentSampleが含まれる。Cordova APKにはいずれも含まれない。
- aaptで両APKのManifestを調べ、既存featureのMainActivityがexported=falseであることを確認。
- dev-appのタスク一覧にassembleRelease・bundleReleaseが存在しない。

ビルド・品質チェック・テストは成功しました。dev-appのLintはエラー0、DataExtractionRulesの警告1件です。開発ホストのallowBackup=falseに対してAndroid 12以降の転送設定を求めるもので、警告の抑制や失敗条件の変更はしていません。このホストはDebug専用で、業務データの永続化は追加していません。

既存featureのLogNotTimber、既存ビルドのGradle非推奨警告も残ります。実NFC・別アプリ向けの業務受付・CordovaPluginの結果返却は今回の開発ホストで実装・検証したものではありません。
