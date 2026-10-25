# 機能別Navigation・ViewModelの起動・復元確認（2026-10-25）

[Intent検証の一覧](../README.md) · [現在の仕様](../../../SERVICE_NAVIGATION.md)

以下は機器別の処理ID・画面構成で検証した時点の記録です。現在はサービス単位へ変更し、[ゴルフ場釣り銭機サービスの検証](../golf-service/README.md)へ移行しています。

Activity独自のSTATE_REQUESTを削除し、Navigationと各画面のSavedStateHandleへ保存・復元を集約した後の検証です。実際のCordova Web→KotlinPlugin→明示的Intent→Navigation→ViewModel経路をAPI 35エミュレーターで確認しました。

- nfc.read・nfc.write・camera.captureをWebボタンから直接開き、各画面の「戻る」でCordovaへ復帰しました。
- 同じActivityで3つの処理IDを受け直し、各ViewModelがargs省略・空オブジェクト・日本語と予約文字を含む構造化JSONを保持しました。
- 同じ要求の再受信と不正な要求6種類を確認しました。新要求は履歴を置き換え、システムバック一度でCordovaへ戻りました。
- 3つの画面すべてで、初回Intentをhome、最新Intentを各機能にした状態から回転・プロセス終了／再起動を行いました。最新の画面と引数が復元され、古いhome要求は再実行されませんでした。
- native・プラグインAAR、Cordova Debug／Release APK、両ホストの品質・単体テスト・Android Lint、KDoc生成が成功しました。featureはDebug／Release各17件、dataは各2件です。
- 機器操作は未実装で、今回の検証は要求の振り分け・画面表示・引数復元が対象です。

| 配置先 | 内容 |
| --- | --- |
| [results.json](results.json) | 全体の成功結果、プロセス再生成前後のPID、拒否した入力、テスト件数。 |
| [checks/](checks/README.md) | ビルド・品質・KDoc・APK導入・UI操作のログ。 |
| [ui/](ui/README.md) | 各処理IDの直接起動、args、回転・プロセス再生成、Cordovaへの復帰のUI階層。 |
| [security-alignment.md](security-alignment.md) | Intent受信境界と復元責務の確認。 |
| [intent-handling.patch](intent-handling.patch) | Intent受信・型付き要求・Navigationへの受け渡しの差分。 |

一時的なWebView接続を解除し、端末の回転設定を元に戻し、エミュレーターを終了しました。以前の独自保存方式のログは親フォルダに履歴として保持しています。
