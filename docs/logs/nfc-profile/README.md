# 型番Profile導入の検証

[ドキュメント一覧](../../README.md) · [構成](../../NFC_READER_DESIGN.md) · [読取仕様](../../NFC_READING.md)

2026-10-28に、Repository内で選んだ型番ProfileをReaderへ渡し、通信方式・メモリ構成を解決する経路を検証しました。

| ファイル | 内容 |
| --- | --- |
| [results.json](results.json) | Profileの設定、ホスト・variant別のJUnit件数、ビルド・品質・KDoc生成の結果 |
| [native-check.log](native-check.log) | nativeのDebug／Releaseテスト、開発用APK、data・featureとdev-appのLint、detekt・整形確認、Dokka生成 |
| [cordova-check.log](cordova-check.log) | CordovaのDebug／Release APK、両variantのテスト、appの両variantのLintとcheck |

## 結果

- 各ホストでdata 55件・feature 46件をDebug／Releaseそれぞれで実行し、101種類・計404実行、失敗・エラー・スキップ0件です。
- 従来のSLI UID判定・コマンド・応答検査・解放・取消・業務値の解釈を維持しています。ReaderとProfileの型はdataのinternalで、featureは型番・通信方式を入力しません。
- SLIのProfileにモデル、ISO 15693／NfcV、28ブロック×4バイトがそろって選択されることを確認しました。
- 試験用Profileの幅・容量を変え、応答長・バイト数・切り出し・範囲検査が固定値へ依存しないことを確認しました。試験の形状は実製品の仕様や新しい対応製品ではありません。
- 実タグの技術・モデルと合わない場合、容量や実装済みコマンドの上限を超える場合は、接続前に拒否します。メタデータ取得時の権限拒否は具体エラーとして返し、予期しない実装例外は伝播します。
- 実タグとの通信は未確認です。今回の変更ではエミュレーターを起動していません。画面接続の前回確認は[汎用検出の記録](../nfc-detection/README.md)にあります。

## 再実行

両ホストはdata・featureのbuild出力を共有します。並行実行せず、ホスト切替時に共有モジュールをcleanします。

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
