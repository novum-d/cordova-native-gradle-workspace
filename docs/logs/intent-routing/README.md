# Intentの処理ID分岐の検証（2026-10-25）

[ログ一覧](../README.md) · [仕様・呼び出し方](../../NFC_NAVIGATION.md)

以下はActivity側で要求JSONを独自保存していた段階の記録です。現在のNavigation・ViewModelへの集約後の記録は[viewmodel-navigation/](viewmodel-navigation/README.md)を参照してください。

Web→KotlinPlugin→明示的Intent→MainActivity→型付き画面の実際の経路を、API 35エミュレーターで確認しました。検証用のWebView接続・回転設定は後片付けし、エミュレーターも終了しています。

| ファイル | 内容 |
| --- | --- |
| [results.json](results.json) | 処理ID分岐・不正要求・新Intent・保存復元と単体テストの結果。 |
| [native-build-tests.log](native-build-tests.log) | native・プラグインのビルド、Debug／Release単体テスト、静的解析・整形確認。 |
| [plugin-reinstall.log](plugin-reinstall.log) | 変更したプラグインJSの再導入。 |
| [cordova-debug-build.log](cordova-debug-build.log) | Cordova Debug APKビルド。 |
| [cordova-release-check.log](cordova-release-check.log) | Cordova Release APKビルド、単体テスト、静的解析・整形確認。 |
| [direct-home-ui.json](direct-home-ui.json) | home処理IDによるホームの直接起動。 |
| [direct-nfc-ui.json](direct-nfc-ui.json) | Webボタンからnfc.readとargs={}をIntentで渡した直接起動。 |
| [omitted-args-ui.json](omitted-args-ui.json) | 同じActivityへの新しいIntentと、args省略の受信。 |
| [structured-args-ui.json](structured-args-ui.json) | 日本語・予約文字・数値・ネストしたargsを受信したNFC画面。 |
| [invalid-request-ui.json](invalid-request-ui.json) | 不正要求が拒否され、表示中の画面とargsが保持されている状態。 |
| [new-intent-home-ui.json](new-intent-home-ui.json) | 新しいIntentのhome処理IDでホームへ切り替えた状態。 |
| [rotation-restored-ui.json](rotation-restored-ui.json) | 最新の受信要求を画面回転後に復元した状態。 |
| [process-restored-ui.json](process-restored-ui.json) | 最新の要求をプロセス終了・再起動後に復元した状態。 |
| [returned-cordova-ui.json](returned-cordova-ui.json) | システムバックでCordovaへ復帰した状態。 |
| [security-alignment.md](security-alignment.md) | Intent受信境界の実装・検証報告。 |
| [intent-handling.patch](intent-handling.patch) | 受信境界を担う3クラスの標準形式差分。 |
