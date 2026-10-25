# WebMessage双方向通信の検証記録

[ログ一覧](../README.md) · [実装・操作手順](../../WEBVIEW_BRIDGE_SAMPLE.md)

確認日: 2026-10-25。API 35 / arm64のCordova_Compose_API_35、Android System WebView 124.0.6367.219で確認しました。インストールしたDebug APKのホストはCordovaComposeAppです。実NFC・カメラ・既存Vueは使用していません。

## 記録ファイル

| ファイル | 内容 |
| --- | --- |
| [native-libraries.log](native-libraries.log) | native単独のfeature・data・cordova-plugin-dev Debug AARビルド |
| [android-checks.log](android-checks.log) | native・Cordovaのcheck、detekt・整形、両variantの単体テスト・Android Lint |
| [cordova-debug-build.log](cordova-debug-build.log) | 最終Debug APKのビルド |
| [cordova-release-build.log](cordova-release-build.log) | 最終Release AABのビルド。署名・公開・配布は行わない |
| [kdoc.log](kdoc.log) | KDoc HTML生成 |
| [build-results.json](build-results.json) | 端末環境・variant別のテスト件数・成果物のSHA-256 |
| [runtime-results.json](runtime-results.json) | 通信・安全な表示・入力拒否・API失敗・reload・ライフサイクルの確認結果 |
| [final-smoke.json](final-smoke.json) | 最終APKで通信・Logcat・再接続・破棄を確認した結果 |
| [final-logcat.log](final-logcat.log) | 最終APKの公開サンプルユーザー受信ログ |
| [connected-user-and-nfc.png](connected-user-and-nfc.png) | Web・Composeのユーザー情報、疑似タグID、CustomEvent発火回数の表示 |
| [before-ready-layout.json](before-ready-layout.json) | JavaScript実行を止めたときのREADY待機・送信0回の実UI |
| [api-failure-layout.json](api-failure-layout.json) | API要求を遮断したときの表示。接続は維持し、失敗をWebへ表示 |
| [final-api-layout.json](final-api-layout.json) | 最終APKでユーザー情報がComposeに表示されたUI |
| [final-reconnected-layout.json](final-reconnected-layout.json) | 最終APKでreload後の疑似NFCイベントが再度到着したUI |

## 確認内容と方法

- 起動は実際のCordova WebボタンからKotlinPlugin・Intentを経由。home・ゴルフの既存画面も確認。
- CDPでサンプルの「API実行」ボタンを操作し、指定URLへ実際にfetch。ComposeのID・名前とDebug Logcatを確認。
- ネイティブの「NFCイベント送信」をUI座標から操作し、HTMLのタグIDと独立したCustomEvent購読側の回数を確認。
- 不正JSON、未知type、数値文字列ID、古いsession、深すぎるJSON、大きすぎる文字列、ArrayBufferを実際のブリッジへ送り、状態を壊さないことを確認。JVMテストでも境界・偽装URLを検証。
- 検証中だけfetchを差し替え、HTMLタグを含む名前が文字列で表示され、img要素もscript実行も発生しないことを確認。その後、元のfetchへ戻して実APIを再実行。
- 同じCDP接続上のNetwork.setBlockedURLsでAPIを遮断し、失敗表示・再試行を確認。端末全体の通信設定は変更しない。
- ハッシュ未許可のscript、外部HTTPS、別assets、file URLへの遷移、別通信先fetchを拒否。
- ネイティブreloadとJSのlocation.reloadでsessionが変わり、古いREADY・結果を除外して再接続することを確認。
- CDPでJavaScriptの実行を一時停止してREADY待機状態を作り、NFCボタンをタップしても送信回数が増えないことを確認。実行を戻してreloadすると接続復帰。
- Activity回転と、バックグラウンドでのam kill後のプロセス再生成で、通信サンプルのルートを復元し、新しい接続を確立。
- 戻る操作後、DevToolsのページ一覧からサンプルWebViewが消えることを確認。端末の回転設定・検証用接続は片付ける。

CDPのRuntime.evaluateは検証の外部操作です。アプリの実装にはevaluateJavascriptや検証用Intent・バックドアを追加していません。

ビルド・静的解析・単体テスト・Lintは成功しました。featureの単体テストはDebug／Release各26件、失敗0件です。featureのLintに残るLogNotTimberは既存MainActivityの1件で、新規WebViewコードには指摘がありません。Gradle 9向けの既存非推奨警告も残っています。

WebMessage非対応の実WebView、実機固有の動作、レンダラープロセス終了時の表示は未検証です。これらは[サンプル文書の残存範囲](../../WEBVIEW_BRIDGE_SAMPLE.md#残っている範囲)に記載しています。
