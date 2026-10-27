# 汎用NFC検出・業務Repositoryの検証

[ドキュメント一覧](../../README.md) · [構成](../../NFC_READER_DESIGN.md) · [読取仕様](../../NFC_READING.md)

2026-10-27に、汎用タグ検出・対象に紐づくRepository・業務ユースケース・画面の接続を検証しました。

| ファイル | 内容 |
| --- | --- |
| [results.json](results.json) | ホスト・variant別のJUnit件数、ビルド・品質チェック・実行確認の結果 |
| [native-check.log](native-check.log) | nativeの両variantのテスト、開発用APK、data・featureの両variantとdev-appのLint、detekt・整形確認、Dokka生成 |
| [cordova-check.log](cordova-check.log) | CordovaのDebug・Release APK、両variantのテスト、appの両variantのLintとcheck |
| [dev-layout.json](dev-layout.json) | 開発用APKで、NFC非搭載時の案内と処理IDを表示 |
| [dev-resumed-layout.json](dev-resumed-layout.json) | 開発用画面を背面へ移動し、前面へ戻した後の案内 |
| [cordova-layout.json](cordova-layout.json) | Cordovaのボタン→プラグイン→Composeの読取画面と、NFC非搭載の案内 |

## 結果

- 各ホストでdata 41件・feature 46件をDebug／Releaseそれぞれで実行し、計348件、失敗・エラー・スキップ0件です。87種類のテストを4通りの構成で実行した件数です。
- dataで検出購読・取消・再購読・直列化・処理中の追加検出破棄・処理終了時の参照失効・具体エラー生成、業務座標・未対応タグ拒否・応答検査・接続解放を確認しました。
- featureで引数なしの読取、同じカードの2項目取得、最初の失敗での停止、失敗後の再検出、具体エラーと元の原因の保持、取消伝播を確認しました。
- API 35エミュレーターで開発用APKとCordova Debugの画面表示を確認しました。アプリのAndroidRuntimeクラッシュはありません。確認後にエミュレーターを終了しました。
- 実ICODE SLIとの通信、NFC搭載端末でのReaderMode・NFC設定変更・離脱と再提示は未確認です。ブロッキングI/Oの即時キャンセルは保証しません。

## 再実行

ビルド出力を共有するため、nativeとCordovaは並行実行せず、ホストを切り替える際にdata・featureをcleanします。

```sh
./scripts/with-env.sh ./native/gradlew -p native \
  :data:clean :feature:clean \
  :data:testDebugUnitTest :data:testReleaseUnitTest \
  :feature:testDebugUnitTest :feature:testReleaseUnitTest :dev-app:assembleDebug \
  :data:lintDebug :data:lintRelease :feature:lintDebug :feature:lintRelease :dev-app:lintDebug \
  detekt detektFormatCheck dokkaGeneratePublicationHtml --console=plain

./scripts/with-env.sh ./native/gradlew -p CordovaComposeApp/platforms/android \
  :data:clean :feature:clean :app:clean :app:assembleDebug :app:assembleRelease \
  :data:testDebugUnitTest :data:testReleaseUnitTest \
  :feature:testDebugUnitTest :feature:testReleaseUnitTest \
  :app:lintDebug :app:lintRelease :check --console=plain
```
