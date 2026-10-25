# ホーム・NFC画面の検証記録（2026-10-25）

[ログ一覧](../README.md) · [画面構成・型付き引数](../../SERVICE_NAVIGATION.md)

このフォルダは、ホーム内のボタンからNFC画面へ遷移する初期サンプルの記録です。現在は[Intentの処理IDによる直接起動](../../SERVICE_NAVIGATION.md)へ変更しており、当時の画面画像・起動経路とは異なります。

Navigation Compose 2.8.9の型付きルートで、ホームとNFC画面の表示・引数・戻る操作・保存復元を確認しました。実際のNFC読み取りは対象外です。

| ファイル | 内容 |
| --- | --- |
| [results.json](results.json) | ビルド・引数テスト・エミュレーター操作の確認結果。 |
| [home.png](home.png) | ホーム画面の表示。 |
| [nfc.png](nfc.png) | 処理IDと省略されたargsを表示するNFC画面。 |
| [home-ui.json](home-ui.json) | ホーム画面のUI階層。 |
| [nfc-omitted-ui.json](nfc-omitted-ui.json) | args省略でNFC画面へ遷移したUI階層。 |
| [nfc-empty-ui.json](nfc-empty-ui.json) | argsに空オブジェクトを指定したUI階層。 |
| [nfc-rotation-ui.json](nfc-rotation-ui.json) | 画面回転後のNFC画面と引数の保持。 |
| [nfc-process-ui.json](nfc-process-ui.json) | プロセス終了・再起動後に復元したNFC画面と引数。 |
| [cordova-return-ui.json](cordova-return-ui.json) | NFC→ホーム→Cordovaへシステムバックで戻ったUI階層。 |
| [native-build-tests.log](native-build-tests.log) | nativeのビルド・引数テスト・静的解析・整形チェック。 |
| [cordova-debug-build.log](cordova-debug-build.log) | CordovaのDebug APKビルド。 |
| [cordova-release-check.log](cordova-release-check.log) | CordovaのRelease APKビルド・引数テスト・静的解析・整形チェック。 |
| [app-crash.log](app-crash.log) | 復元後のアプリPIDに限定したcrashバッファ。 |

起動したAPI 35エミュレーターは検証後に終了しました。画面回転の設定も元に戻しています。UI記録に一時的な遷移元の要素が含まれる場合がありますが、戻る操作の完了確認では遷移先の画面名と遷移元の消失を確認しています。
