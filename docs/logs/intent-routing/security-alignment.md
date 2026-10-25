# Best Practices and Security Alignment Update: Intentの起動要求

[検証記録一覧](README.md) · [画面と引数](../../NFC_NAVIGATION.md)

- **改善領域**: 受信Intentの型付きJSON検証、onCreate／onNewIntentの共通処理。
- **影響・優先度**: 低。既存の非公開Activityへ渡す新しい起動要求について、不正な処理IDやargsで別画面を開かないようにする。
- **変更対象**: MainActivity、KotlinPlugin、新規FeatureLaunchRequest。関連するJSブリッジ・サンプルWeb・Navigationの起動先も変更した。Manifestのexported=falseと既存の依存構成は維持した。
- **実装**: KotlinPluginで単一の要求オブジェクトを復元・検証し、固定コンポーネントへの明示的IntentへJSON文字列を格納する。Activityは同じ型へ復元し、既知の処理IDだけを型付き画面へ変換する。onNewIntentではsetIntentの後に同じ検証を適用し、最後に受け付けたJSONを保存状態へ残す。
- **検証**: 必須idの欠落、数値id、未知ID、配列args、要求全体がnullの場合を拒否することを確認した。同じActivityRecordでの画面切り替えと、最新要求の画面回転・プロセス再生成後の復元を確認した。

[標準形式の実装差分](intent-handling.patch)に、受信境界を担う3クラスの変更を保存しています。MainActivityの差分には、直前のナビゲーション導入からの未コミット変更も含みます。

参照したスキルはローカルの`android-intent-security`です。[AndroidのActivity仕様](https://developer.android.com/reference/android/app/Activity#onNewIntent(android.content.Intent))に従って、新しいIntentと初回起動を同じ検証経路へ渡しています。
