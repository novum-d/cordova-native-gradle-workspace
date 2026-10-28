# カードRepositoryの生成・実装分離の検証

[ドキュメント一覧](../../README.md) · [構成とファイル配置](../../NFC_READER_DESIGN.md)

2026-10-28に、業務生成のCardRepositoryFactory、共通読取のNfcCardRepositoryFactory、各Repository実装の分離を検証しました。

| ファイル | 内容 |
| --- | --- |
| [results.json](results.json) | ホスト・variant別のテスト件数、ビルド・品質・ファイル分離の確認結果 |
| [native-check.log](native-check.log) | nativeのDebug／Releaseテスト、開発用APK、data・featureとdev-appのLint、detekt・整形、Dokka生成 |
| [cordova-check.log](cordova-check.log) | CordovaのDebug／Release APK、両variantのテスト、appの両variantのLintとcheck |

## 結果

- 各ホストでdata 59件・feature 46件をDebug／Releaseそれぞれで実行しました。105種類・計420実行で、失敗・エラー・スキップは0件です。
- 業務項目によらない共通の範囲読取、領域外の接続前拒否、同じタグを指す別Repository間の排他、各生成時のエラーFactoryの独立性を追加検証しました。
- 既存のゴルフ2項目の座標、Profileの選択、タグ参照の失効、具体エラーと原因の保持、キャンセル、失敗後の再検出は維持しています。
- FactoryのファイルへRepository実装を宣言していないことと、featureへモデル・Profile・通信方式の型を公開していないことを確認しました。テスト用の生成役・模擬Repositoryも別ファイルです。
- 社員証等の仕様未確定の例示用実装はありません。実タグの通信は未確認で、今回はエミュレーターを起動していません。

## 再実行

両ホストはdata・featureのbuild出力を共有するため、並行実行せず、ホスト切替時に共有モジュールをcleanします。

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
