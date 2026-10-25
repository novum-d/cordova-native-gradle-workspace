# ゴルフ場釣り銭機サービスの画面起動（2026-10-25）

[Intent検証一覧](../README.md) · [現在の仕様](../../../SERVICE_NAVIGATION.md)

機器名ごとのNavigationを廃止し、services/golfchangeへサービスの画面・ViewModel・Navigationをまとめました。処理IDはgolf-change.read-holderです。確定した読み取り対象はゴルフ場ID・ホルダー番号で、値の取得・解析・Webへの結果返却は今回の実装に含みません。

- 実際のWebボタン→KotlinPlugin→Intent→Navigation→ViewModelでサービス画面を起動しました。
- 同じActivityでargs省略・空オブジェクト・日本語と予約文字を含む構造化JSONを受け取りました。
- 旧IDのnfc.read・nfc.write・camera.captureと、不正な要求を拒否しました。NFC書き込み画面・ルート・ViewModelは削除しています。
- 同じ要求の再受信、戻る操作によるCordova復帰、回転とプロセス終了・再起動後の最新画面・argsの復元を確認しました。
- native・プラグインのビルド、featureのDebug／Release単体テスト各17件、CordovaのDebug APK／Release AAB、両ホストの品質・Lintが成功しました。

| 配置先 | 内容 |
| --- | --- |
| [results.json](results.json) | 対象サービス、実装範囲、引数・旧ID拒否・復元・単体テストの結果。 |
| [checks/](checks/README.md) | ビルド・品質・APK導入・UI操作のログ。 |
| [ui/](ui/README.md) | サービス画面、各args、回転・プロセス再生成、Cordova復帰のUI階層。 |

本人確認（マイナンバー）は、仕様確定後に別サービスとして追加する想定です。

KDoc HTMLの生成も成功しました。一時的なWebView接続を解除し、端末の回転設定を元に戻し、エミュレーターを終了しています。
