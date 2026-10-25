# Best Practices and Security Alignment Update: Intentの振り分けと状態復元

[検証概要](README.md) · [現在の仕様](../../../NFC_NAVIGATION.md)

- **対象・優先度**: 低。同一アプリのCordovaプラグインから非公開Activityへ渡すJSON要求。
- **実装**: KotlinPluginは単一の要求オブジェクトを検証し、既知の処理IDだけを固定コンポーネントへの明示的Intentへ格納する。ネストしたIntentや遷移先クラス名は受け取らない。
- **初回と再受信**: 初回のonCreateとonNewIntentは同じ検証処理を通す。singleTopで再利用するときはsetIntent後に新しい要求を処理する。
- **復元**: 再生成時は古いIntentを再処理せず、Navigationと画面ごとのSavedStateHandleから最新の画面・引数を復元する。Activity独自のSTATE_REQUESTとonSaveInstanceStateは削除した。
- **公開範囲・依存**: Manifestのexported=falseを維持し、外部公開コンポーネントや権限は追加しない。既存版のLifecycle ViewModel SavedStateを直接依存へ明示した。
- **確認結果**: 3処理IDの直接起動・再受信・省略／空／構造化args、6種類の不正要求の拒否、回転とプロセス再生成後の復元をAPI 35で確認した。NFC・Cameraの実機操作は対象外。

[実装差分](intent-handling.patch)はMainActivity、KotlinPlugin、起動要求、Navigation・JSON引数の変更を含みます。参照スキルはローカルのandroid-intent-securityです。参照仕様は[Activity.onNewIntent](https://developer.android.com/reference/android/app/Activity#onNewIntent(android.content.Intent))です。
